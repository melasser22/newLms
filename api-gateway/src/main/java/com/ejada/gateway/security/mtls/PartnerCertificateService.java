package com.ejada.gateway.security.mtls;

import com.ejada.gateway.config.GatewaySecurityProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.security.MessageDigest;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Provides cached lookup of partner TLS certificates bound to tenants.
 */
@Service
public class PartnerCertificateService {

  private static final Logger LOGGER = LoggerFactory.getLogger(PartnerCertificateService.class);
  private static final HexFormat HEX = HexFormat.of().withUpperCase();

  private final PartnerCertificateRepository repository;
  private final GatewaySecurityProperties properties;
  private final Cache<String, List<PartnerCertificateEntity>> cache;

  public PartnerCertificateService(PartnerCertificateRepository repository,
      GatewaySecurityProperties properties) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.properties = Objects.requireNonNull(properties, "properties");
    Duration ttl = properties.getMutualTls().getCacheTtl();
    this.cache = Caffeine.newBuilder()
        .expireAfterWrite(ttl == null ? Duration.ofMinutes(5) : ttl)
        .maximumSize(500)
        .build();
  }

  public Mono<Boolean> isCertificateTrusted(String tenantId, X509Certificate certificate) {
    if (!StringUtils.hasText(tenantId) || certificate == null) {
      return Mono.just(false);
    }
    return Mono.fromCallable(() -> cache.get(tenantId, this::loadCertificates))
        .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
        .flatMapMany(Flux::fromIterable)
        .filter(entity -> matches(certificate, entity))
        .hasElements();
  }

  private List<PartnerCertificateEntity> loadCertificates(String tenantId) {
    return repository.findByTenantIdAndRevokedFalse(tenantId)
        .collectList()
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to load partner certificates for tenant {}", tenantId, ex);
          return Mono.just(List.of());
        })
        .blockOptional(Duration.ofSeconds(5))
        .orElse(List.of());
  }

  private boolean matches(X509Certificate certificate, PartnerCertificateEntity entity) {
    try {
      if (entity.isRevoked()) {
        return false;
      }
      Instant now = Instant.now();
      Instant validFrom = entity.getValidFrom();
      Instant validTo = entity.getValidTo();
      if ((validFrom != null && now.isBefore(validFrom.minus(properties.getMutualTls().getClockSkew())))
          || (validTo != null && now.isAfter(validTo.plus(properties.getMutualTls().getClockSkew())))) {
        return false;
      }
      String fingerprint = fingerprint(certificate);
      return fingerprint.equalsIgnoreCase(entity.getFingerprintSha256());
    } catch (CertificateEncodingException ex) {
      LOGGER.warn("Failed to compute fingerprint for certificate {}", entity.getId(), ex);
      return false;
    }
  }

  private String fingerprint(X509Certificate certificate) throws CertificateEncodingException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (Exception ex) {
      throw new IllegalStateException("SHA-256 digest unavailable", ex);
    }
    byte[] encoded = certificate.getEncoded();
    byte[] hash = digest.digest(encoded);
    return HEX.formatHex(hash);
  }
}
