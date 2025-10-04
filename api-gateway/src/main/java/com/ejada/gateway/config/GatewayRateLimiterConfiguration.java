package com.ejada.gateway.config;

import com.ejada.shared_starter_ratelimit.RateLimitProps;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;

/**
 * Provides the primary {@link KeyResolver} bean used by Spring Cloud Gateway's
 * {@code RequestRateLimiterGatewayFilterFactory}. The resolver is selected based on the
 * configured rate limiting strategy.
 */
@Configuration
@ConditionalOnBean({RateLimitProps.class, KeyResolver.class})
public class GatewayRateLimiterConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRateLimiterConfiguration.class);

  private static final String STRATEGY_TENANT = "tenant";
  private static final String STRATEGY_IP = "ip";
  private static final String STRATEGY_USER = "user";

  @Bean
  @Primary
  public KeyResolver rateLimiterKeyResolver(RateLimitProps props, Map<String, KeyResolver> resolvers) {
    String strategy = normalizeStrategy(props.getKeyStrategy());

    KeyResolver resolver = selectResolver(strategy, resolvers);
    if (resolver != null) {
      LOGGER.info("Using '{}' key resolver for rate limiting", strategy);
      return resolver;
    }

    KeyResolver fallback = selectResolver(STRATEGY_TENANT, resolvers);
    if (fallback != null) {
      LOGGER.warn(
          "No key resolver found for strategy '{}'; falling back to tenant resolver", strategy);
      return fallback;
    }

    if (!resolvers.isEmpty()) {
      KeyResolver firstAvailable = resolvers.values().iterator().next();
      LOGGER.warn(
          "No matching key resolver found for strategy '{}'; using first available resolver {}",
          strategy,
          firstAvailable.getClass().getSimpleName());
      return firstAvailable;
    }

    throw new IllegalStateException("No KeyResolver beans available to satisfy rate limiter configuration");
  }

  private String normalizeStrategy(String strategy) {
    if (!StringUtils.hasText(strategy)) {
      return STRATEGY_TENANT;
    }
    return strategy.trim().toLowerCase(Locale.ROOT);
  }

  private KeyResolver selectResolver(String strategy, Map<String, KeyResolver> resolvers) {
    if (Objects.equals(strategy, STRATEGY_TENANT)) {
      return resolvers.get("tenantKeyResolver");
    }
    if (Objects.equals(strategy, STRATEGY_IP)) {
      return resolvers.get("ipKeyResolver");
    }
    if (Objects.equals(strategy, STRATEGY_USER)) {
      return resolvers.get("userKeyResolver");
    }
    return null;
  }
}
