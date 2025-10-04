package com.ejada.gateway.subscription;

import com.ejada.gateway.config.GatewayOptimizationProperties;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Periodically warms critical subscription caches to avoid cold-start latency.
 */
@Component
public class SubscriptionCacheWarmup {

  private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionCacheWarmup.class);

  private final GatewayOptimizationProperties optimizationProperties;
  private final SubscriptionValidationService validationService;

  public SubscriptionCacheWarmup(GatewayOptimizationProperties optimizationProperties,
      SubscriptionValidationService validationService) {
    this.optimizationProperties = optimizationProperties;
    this.validationService = validationService;
  }

  @PostConstruct
  public void warmupOnStartup() {
    if (!optimizationProperties.isEnabled()) {
      return;
    }
    warmupTenants("startup");
  }

  @Scheduled(fixedDelayString = "PT15M")
  public void scheduledWarmup() {
    if (!optimizationProperties.isEnabled()) {
      return;
    }
    warmupTenants("scheduled");
  }

  private void warmupTenants(String source) {
    List<String> tenants = optimizationProperties.getWarmupTenants();
    if (tenants == null || tenants.isEmpty()) {
      return;
    }
    Duration timeout = optimizationProperties.getWarmupTimeout();
    LOGGER.info("Starting subscription cache warmup from {} for {} tenants", source, tenants.size());
    Flux.fromIterable(tenants)
        .flatMap(validationService::warmupTenant)
        .timeout(timeout)
        .doOnError(ex -> LOGGER.warn("Cache warmup timed out", ex))
        .onErrorResume(ex -> Flux.empty())
        .then()
        .subscribe();
  }
}
