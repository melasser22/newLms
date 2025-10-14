package com.ejada.shared_starter_ratelimit;

import com.ejada.shared_starter_ratelimit.RateLimitProps.BypassProperties;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

/**
 * Evaluates whether a request should bypass rate limiting based on roles.
 */
public class RateLimitBypassEvaluator {

  private final BypassProperties properties;

  public RateLimitBypassEvaluator(BypassProperties properties) {
    this.properties = properties;
  }

  public Optional<RateLimitBypassDecision> evaluate(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return Optional.empty();
    }
    if (!properties.isEnabled()) {
      return Optional.empty();
    }

    List<String> authorities = extractAuthorities(authentication.getAuthorities());
    for (String authority : authorities) {
      if (properties.getSuperAdminRoles().contains(authority)) {
        return Optional.of(new RateLimitBypassDecision(RateLimitBypassType.SUPER_ADMIN, authority));
      }
      if (properties.getSystemIntegrationRoles().contains(authority)) {
        return Optional.of(new RateLimitBypassDecision(RateLimitBypassType.SYSTEM_INTEGRATION, authority));
      }
    }
    return Optional.empty();
  }

  private List<String> extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
    if (authorities == null) {
      return List.of();
    }
    return authorities.stream()
        .flatMap(authority -> Stream.ofNullable(authority.getAuthority()))
        .map(value -> value.trim().toUpperCase(Locale.ROOT))
        .toList();
  }
}
