package com.ejada.crypto.starter;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import com.ejada.crypto.starter.InMemoryKeyProviderAutoConfiguration;

import com.ejada.crypto.AesGcmCrypto;
import com.ejada.crypto.CryptoAlgorithm;
import com.ejada.crypto.CryptoFacade;
import com.ejada.crypto.HmacSigner;
import com.ejada.crypto.AesGcmEncryptor;
import com.ejada.crypto.HmacSha256Signer;
import com.ejada.crypto.Encryptor;
import com.ejada.crypto.Signer;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

@AutoConfiguration(after = InMemoryKeyProviderAutoConfiguration.class)
@EnableConfigurationProperties(CryptoProperties.class)
@ConditionalOnClass({CryptoFacade.class, CryptoAlgorithm.class})
@ConditionalOnProperty(prefix = "shared.crypto", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CryptoAutoConfiguration {

  private static final Logger log = LoggerFactory.getLogger(CryptoAutoConfiguration.class);

  // Use the shared AesGcm implementation by default
  @Bean
  @ConditionalOnMissingBean(CryptoAlgorithm.class)
  public CryptoAlgorithm cryptoAlgorithm(CryptoProperties props) {
    return switch (props.getAlgorithm()) {
      case AES_GCM -> new AesGcmCrypto();
      // If/when you add an implementation, swap this to `new ChaChaPolyCrypto()`
      case CHACHA20_POLY1305 ->
          throw new IllegalArgumentException("CHACHA20_POLY1305 not supported yet—please add implementation or set shared.crypto.algorithm=AES_GCM");
    };
  }

  /**
   * Main CryptoFacade bean using the new Encryptor/Signer APIs.
   * It resolves both AES and HMAC keys from the single-key provider’s current key.
   */
  @Bean
  @ConditionalOnBean(InMemoryKeyProviderAutoConfiguration.KeyProvider.class)
  @ConditionalOnMissingBean
  public CryptoFacade  cryptoFacade(
      CryptoAlgorithm algorithm,
      InMemoryKeyProviderAutoConfiguration.KeyProvider keyProvider,
      ObjectProvider<MeterRegistry> meters) {

    Supplier<SecretKey> currentKeySupplier = keyProvider::getCurrentKey;

    Encryptor encryptor = new AesGcmEncryptor(algorithm, currentKeySupplier);
    Signer signer = new HmacSha256Signer(currentKeySupplier);
    CryptoFacade facade = new CryptoFacade(encryptor, signer);
    
    // optional Micrometer wiring (only if registry present)
    meters.ifAvailable(reg -> {
      MeterBinder binder = r -> {
        // Simple gauge: do we have an active key?
        r.gauge("crypto.keys.active", keyProvider,
            p -> p.currentKeyId() != null ? 1.0 : 0.0);
      };
      binder.bindTo(reg);
    });

    log.info("Shared CryptoFacade  initialized (alg={}, provider=current-key)",
        algorithm.getClass().getSimpleName());
    return facade;
  }

  /** Convenience HMAC signer bean (shared lib), if someone wants to inject it directly.
   *
   * <p>This bean is only created when no other {@code HmacSigner} is present and no
   * {@link InMemoryKeyProviderAutoConfiguration.KeyProvider} is registered. When the
   * in-memory key provider is active, a more advanced signer is supplied there and this
   * fallback should be skipped to avoid bean name collisions.</p>
   */
  @Bean
  @ConditionalOnMissingBean({HmacSigner.class, InMemoryKeyProviderAutoConfiguration.KeyProvider.class})
  public HmacSigner hmacSigner() {
    return new HmacSigner(); // HmacSHA256 helper for legacy usage
  }

  /**
   * Health indicator: checks that a current key exists and its byte length is sane.
   * Uses the single-key provider (no KeyMaterial / SPI).
   */
  @Bean
  @ConditionalOnBean(InMemoryKeyProviderAutoConfiguration.KeyProvider.class)
  @ConditionalOnMissingBean(name = "cryptoHealthIndicator")
  public HealthIndicator cryptoHealthIndicator(InMemoryKeyProviderAutoConfiguration.KeyProvider provider) {
    return () -> {
      try {
        SecretKey key = provider.getCurrentKey();
        if (key == null) {
          return Health.down().withDetail("reason", "no-active-key").build();
        }
        byte[] enc = key.getEncoded(); // SecretKeySpec exposes bytes; could be null for some providers
        int len = enc == null ? 0 : enc.length;
        boolean okLen = len >= 16; // basic sanity

        return (okLen ? Health.up() : Health.unknown())
            .withDetail("kid", Objects.toString(provider.currentKeyId(), "<none>"))
            .withDetail("len", len)
            .withDetail("checkedAt", Instant.now().toString())
            .build();
      } catch (Exception e) {
        return Health.down(e).build();
      }
    };
  }

  /** Optional: log active kid on startup (no secrets). */
  @Bean
  @ConditionalOnProperty(prefix = "shared.crypto", name = "log-active-kid", havingValue = "true", matchIfMissing = false)
  @ConditionalOnBean(InMemoryKeyProviderAutoConfiguration.KeyProvider.class)
  public Runnable cryptoStartupLogger(InMemoryKeyProviderAutoConfiguration.KeyProvider provider) {
    return () -> {
      try {
        log.info("shared-crypto: active KID is {}", Objects.toString(provider.currentKeyId(), "<none>"));
      } catch (Exception e) {
        log.warn("shared-crypto: cannot read active KID: {}", e.toString());
      }
    };
  }
}
