package com.ejada.gateway.cache;

import com.ejada.gateway.config.GatewayCacheProperties;
import com.ejada.gateway.transformation.ResponseCacheService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Listens for downstream domain events and invalidates cached responses when
 * catalog or tenant data changes.
 */
@Component
@Lazy(false)
@ConditionalOnExpression("${gateway.cache.enabled:true} && ${gateway.cache.kafka.enabled:true}")
public class CacheInvalidationListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheInvalidationListener.class);

  private final GatewayCacheProperties properties;

  private final ResponseCacheService cacheService;

  private final ObjectMapper objectMapper;

  public CacheInvalidationListener(GatewayCacheProperties properties,
      ResponseCacheService cacheService,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.cacheService = cacheService;
    this.objectMapper = objectMapper;
  }

  @KafkaListener(topics = "${gateway.cache.topics.tenant-updated:tenant.updated}",
      groupId = "${gateway.cache.kafka.group-id:gateway-cache}")
  public void onTenantUpdated(String payload) {
    if (!properties.isEnabled()) {
      return;
    }
    extractField(payload, "tenantId")
        .ifPresentOrElse(tenant -> invalidateTenantCaches(tenant),
            () -> LOGGER.debug("tenant.updated event missing tenantId"));
  }

  @KafkaListener(topics = "${gateway.cache.topics.catalog-plan-updated:catalog.plan.updated}",
      groupId = "${gateway.cache.kafka.group-id:gateway-cache}")
  public void onCatalogPlanUpdated(String payload) {
    if (!properties.isEnabled()) {
      return;
    }
    invalidateRoute("catalog-plans");
    invalidateRoute("catalog-features");
  }

  private void invalidateTenantCaches(String tenantId) {
    properties.getRouteById("tenant-by-id")
        .ifPresent(route -> cacheService.invalidateRouteForTenant(route, tenantId)
            .doOnError(ex -> LOGGER.warn("Failed to invalidate tenant cache for {}", tenantId, ex))
            .subscribe());
    properties.getRouteById("catalog-plans")
        .ifPresent(route -> cacheService.invalidateRouteForTenant(route, tenantId)
            .subscribe());
    properties.getRouteById("catalog-features")
        .ifPresent(route -> cacheService.invalidateRouteForTenant(route, tenantId)
            .subscribe());
  }

  private void invalidateRoute(String routeId) {
    properties.getRouteById(routeId)
        .ifPresent(route -> cacheService.invalidateRoute(route)
            .doOnError(ex -> LOGGER.warn("Failed to invalidate cache route {}", routeId, ex))
            .subscribe());
  }

  private Optional<String> extractField(String payload, String fieldName) {
    if (!StringUtils.hasText(payload)) {
      return Optional.empty();
    }
    try {
      JsonNode node = objectMapper.readTree(payload);
      JsonNode field = node.path(fieldName);
      if (field.isMissingNode() || !field.isTextual()) {
        return Optional.empty();
      }
      String value = field.asText();
      return StringUtils.hasText(value) ? Optional.of(value) : Optional.empty();
    } catch (Exception ex) {
      LOGGER.debug("Failed to parse cache invalidation payload", ex);
      return Optional.empty();
    }
  }
}
