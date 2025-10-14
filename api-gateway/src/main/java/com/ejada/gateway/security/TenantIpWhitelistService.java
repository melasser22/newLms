package com.ejada.gateway.security;

import com.ejada.gateway.config.GatewaySecurityProperties;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Manages tenant specific IP whitelist entries backed by Redis.
 */
@Service
public class TenantIpWhitelistService {

  private final ReactiveStringRedisTemplate redisTemplate;
  private final GatewaySecurityProperties properties;

  public TenantIpWhitelistService(ReactiveStringRedisTemplate redisTemplate,
      GatewaySecurityProperties properties) {
    this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate");
    this.properties = Objects.requireNonNull(properties, "properties");
  }

  public Flux<String> list(String tenantId) {
    String key = properties.getIpFiltering().whitelistKey(tenantId);
    return redisTemplate.opsForSet().members(key)
        .filter(StringUtils::hasText)
        .map(String::trim);
  }

  public Mono<Void> replace(String tenantId, Collection<String> entries) {
    String key = properties.getIpFiltering().whitelistKey(tenantId);
    Set<String> normalised = normalise(entries);
    return redisTemplate.delete(key)
        .thenMany(Flux.fromIterable(normalised)
            .flatMap(value -> redisTemplate.opsForSet().add(key, value).then()))
        .then();
  }

  public Mono<Void> add(String tenantId, String entry) {
    String key = properties.getIpFiltering().whitelistKey(tenantId);
    String normalised = normalise(entry);
    if (!StringUtils.hasText(normalised)) {
      return Mono.error(new IllegalArgumentException("Invalid IP or CIDR entry"));
    }
    return redisTemplate.opsForSet().add(key, normalised).then();
  }

  public Mono<Void> remove(String tenantId, String entry) {
    String key = properties.getIpFiltering().whitelistKey(tenantId);
    String normalised = normalise(entry);
    if (!StringUtils.hasText(normalised)) {
      return Mono.empty();
    }
    return redisTemplate.opsForSet().remove(key, normalised).then();
  }

  private Set<String> normalise(Collection<String> entries) {
    if (entries == null) {
      return Set.of();
    }
    return entries.stream()
        .map(this::normalise)
        .filter(StringUtils::hasText)
        .collect(Collectors.toUnmodifiableSet());
  }

  private String normalise(String entry) {
    if (!StringUtils.hasText(entry)) {
      return null;
    }
    String candidate = entry.trim();
    try {
      int slash = candidate.indexOf('/');
      if (slash > 0) {
        String ip = candidate.substring(0, slash);
        String mask = candidate.substring(slash + 1);
        int prefix = Integer.parseInt(mask);
        InetAddress address = InetAddress.getByName(ip);
        int bits = address.getAddress().length * 8;
        if (prefix < 0 || prefix > bits) {
          return null;
        }
        return address.getHostAddress() + "/" + prefix;
      }
      InetAddress address = InetAddress.getByName(candidate);
      return address.getHostAddress();
    } catch (Exception ex) {
      return null;
    }
  }
}
