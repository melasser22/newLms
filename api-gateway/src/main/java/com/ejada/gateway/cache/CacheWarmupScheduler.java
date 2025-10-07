package com.ejada.gateway.cache;

import com.ejada.gateway.config.GatewayCacheProperties;
import com.ejada.gateway.config.GatewayCacheProperties.RouteCacheProperties;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * Periodically warms cacheable routes to reduce cold-start latency for tenants.
 */
@Component
public class CacheWarmupScheduler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheWarmupScheduler.class);

  private final GatewayCacheProperties properties;

  private final CacheRefreshService cacheRefreshService;

  public CacheWarmupScheduler(GatewayCacheProperties properties,
      CacheRefreshService cacheRefreshService) {
    this.properties = properties;
    this.cacheRefreshService = cacheRefreshService;
  }

  @Scheduled(fixedDelayString = "${gateway.cache.warm-interval:PT15M}")
  public void warm() {
    if (!properties.isEnabled()) {
      return;
    }
    List<RouteCacheProperties> warmable = properties.getRoutes().stream()
        .filter(RouteCacheProperties::isWarm)
        .toList();
    if (warmable.isEmpty()) {
      return;
    }
    List<String> tenants = properties.getWarmTenants();
    if (CollectionUtils.isEmpty(tenants)) {
      tenants = List.of("anonymous");
    }
    for (RouteCacheProperties route : warmable) {
      List<String> tenantTargets = route.isTenantScoped()
          ? tenants
          : List.of("global");
      for (String tenant : tenantTargets) {
        resolveWarmPath(route, tenant)
            .ifPresent(path -> cacheRefreshService.warmRoute(route, path, tenant)
                .doOnError(ex -> LOGGER.debug("Warmup call failed for route {} tenant {}", route.getId(), tenant, ex))
                .onErrorResume(ex -> Mono.empty())
                .subscribe());
      }
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void warmOnStartup() {
    warm();
  }

  private Optional<String> resolveWarmPath(RouteCacheProperties route, String tenant) {
    String path = route.normalisedWarmPath();
    if (!StringUtils.hasText(path)) {
      return Optional.empty();
    }
    if (route.hasPathVariable()) {
      if (!StringUtils.hasText(tenant) || "anonymous".equalsIgnoreCase(tenant)) {
        return Optional.empty();
      }
      String sanitizedTenant = tenant.toLowerCase(Locale.ROOT);
      path = path.replaceAll("\\{[^}]+\\}", sanitizedTenant);
    }
    return Optional.of(path);
  }
}
