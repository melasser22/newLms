package com.ejada.gateway.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.ejada.gateway.subscription.SubscriptionCacheService;
import com.ejada.gateway.subscription.SubscriptionRecord;
import com.ejada.shared_starter_ratelimit.RateLimitProps;
import java.util.Optional;
import org.mockito.Answers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

/**
 * Provides lightweight test doubles for subscription cache interactions.
 * Many WebFlux slices only need the presence of the bean and do not exercise
 * the real Redis-backed logic. This configuration supplies relaxed Mockito
 * mocks that behave predictably without pulling additional infrastructure
 * into the application context.
 */
@Configuration
public class SubscriptionCacheTestConfiguration {

  @Bean
  @Primary
  public SubscriptionCacheService testSubscriptionCacheService() {
    SubscriptionCacheService cacheService = mock(SubscriptionCacheService.class, Answers.RETURNS_SMART_NULLS);
    lenient().when(cacheService.getCached(anyString())).thenReturn(Mono.just(Optional.empty()));
    lenient().when(cacheService.fetchAndCache(anyString())).thenReturn(Mono.just(SubscriptionRecord.inactive()));
    lenient().when(cacheService.cacheTenant(anyString(), any(SubscriptionRecord.class))).thenReturn(Mono.empty());
    lenient().when(cacheService.cache(anyString(), any(SubscriptionRecord.class))).thenReturn(Mono.empty());
    return cacheService;
  }

  @Bean
  @Primary
  public RateLimitProps testRateLimitProps() {
    return new RateLimitProps();
  }
}
