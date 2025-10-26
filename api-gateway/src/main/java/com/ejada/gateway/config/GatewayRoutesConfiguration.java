package com.ejada.gateway.config;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.ejada.gateway.filter.ApiVersioningGatewayFilter;
import com.ejada.gateway.filter.RequestBodyTransformationGatewayFilterFactory;
import com.ejada.gateway.filter.ResponseBodyTransformationGatewayFilterFactory;
import com.ejada.gateway.filter.SessionAffinityGatewayFilter;
import com.ejada.gateway.resilience.TenantCircuitBreakerMetrics;
import com.ejada.gateway.versioning.VersionNormalizationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.factory.RequestSizeGatewayFilterFactory;
import org.springframework.cloud.gateway.filter.factory.SpringCloudCircuitBreakerFilterFactory;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.BooleanSpec;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;

/**
 * Programmatic route configuration so we can reuse the shared properties and
 * enforce consistent filters across environments.
 */
@Configuration
@EnableConfigurationProperties(GatewayRoutesProperties.class)
public class GatewayRoutesConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayRoutesConfiguration.class);

  private static final Duration PROVIDER_TIMEOUT = Duration.ofSeconds(5);

  @Bean
  @RefreshScope
  RouteLocator gatewayRoutes(RouteLocatorBuilder builder,
      GatewayRoutesProperties properties,
      ObjectProvider<GatewayRouteDefinitionProvider> dynamicProviders,
      ObjectProvider<RequestBodyTransformationGatewayFilterFactory> requestTransformationFactory,
      ObjectProvider<ResponseBodyTransformationGatewayFilterFactory> responseTransformationFactory,
      ObjectProvider<RequestSizeGatewayFilterFactory> requestSizeFilterFactory,
      ObjectProvider<VersionNormalizationFilter> versionNormalizationFilter,
      ObjectProvider<SpringCloudCircuitBreakerFilterFactory> circuitBreakerFilterFactory,
      TenantCircuitBreakerMetrics circuitBreakerMetrics) {
    RouteLocatorBuilder.Builder routes = builder.routes();

    Map<String, GatewayRoutesProperties.ServiceRoute> aggregated = new LinkedHashMap<>();
    AtomicInteger staticRouteCount = new AtomicInteger();
    AtomicInteger dynamicRouteCount = new AtomicInteger();

    properties.getRoutes().forEach((key, route) -> {
      route.applyDefaults(properties.getDefaults());
      route.validate(key);
      GatewayRoutesProperties.ServiceRoute previous = aggregated.put(route.getId(), route);
      if (previous != null) {
        LOGGER.warn("Route {} from static configuration replaced an existing definition", route.getId());
      }
      staticRouteCount.incrementAndGet();
    });

    dynamicProviders.orderedStream().forEach(provider -> {
      try {
        List<GatewayRoutesProperties.ServiceRoute> loaded = provider.loadRoutes()
            .map(route -> {
              route.applyDefaults(properties.getDefaults());
              return route;
            })
            .collectList()
            .block(PROVIDER_TIMEOUT);
        if (loaded == null || loaded.isEmpty()) {
          LOGGER.info("Dynamic route provider {} returned no routes", provider.getProviderName());
          return;
        }
        for (GatewayRoutesProperties.ServiceRoute route : loaded) {
          if (route == null) {
            continue;
          }
          route.validate(route.getId());
          GatewayRoutesProperties.ServiceRoute previous = aggregated.put(route.getId(), route);
          if (previous != null) {
            LOGGER.warn("Route {} from provider {} replaced an existing definition", route.getId(), provider.getProviderName());
          }
        }
        dynamicRouteCount.addAndGet(loaded.size());
        LOGGER.info("Loaded {} dynamic routes from {}", loaded.size(), provider.getProviderName());
      } catch (Exception ex) {
        LOGGER.warn("Failed to load dynamic routes from {}", provider.getProviderName(), ex);
      }
    });

    if (aggregated.isEmpty()) {
      throw new IllegalStateException(
          "No gateway routes were configured. Verify gateway.routes entries in application.yml or the Config Server response.");
    }

    LOGGER.info("Registering {} gateway routes ({} static, {} dynamic)",
        aggregated.size(), staticRouteCount.get(), dynamicRouteCount.get());

    boolean circuitBreakerAvailable = circuitBreakerFilterFactory.getIfAvailable() != null;

    for (GatewayRoutesProperties.ServiceRoute route : aggregated.values()) {
      GatewayRoutesProperties.ServiceRoute.Resilience resilience = route.getResilience();
      if (resilience != null && circuitBreakerMetrics != null) {
        String circuitBreakerName = resilience.resolvedCircuitBreakerName(route.getId());
        TenantCircuitBreakerMetrics.Priority priority = TenantCircuitBreakerMetrics.Priority
            .valueOf(resilience.getPriority().name());
        circuitBreakerMetrics.registerPriority(circuitBreakerName, priority);
      }

      routes.route(route.getId(), predicate -> {
        String[] paths = route.getPaths().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .toArray(String[]::new);

        HttpMethod[] methods = route.getMethods().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .map(String::toUpperCase)
            .map(HttpMethod::valueOf)
            .toArray(HttpMethod[]::new);

        BooleanSpec methodPredicate;
        if (route.getWeight().isEnabled()) {
          methodPredicate = predicate
              .weight(route.getWeight().getGroup(), route.getWeight().getValue())
              .and()
              .path(paths);
        } else {
          methodPredicate = predicate.path(paths);
        }
        if (methods.length > 0) {
          methodPredicate = methodPredicate.and().method(methods);
        }

        return methodPredicate
            .filters(filters -> {
              if (route.getStripPrefix() > 0) {
                filters.stripPrefix(route.getStripPrefix());
              }
              if (StringUtils.hasText(route.getPrefixPath())) {
                filters.prefixPath(route.getPrefixPath());
                GatewayFilter dedupeFilter = deduplicatePrefixedPathFilter(route.getPrefixPath());
                if (dedupeFilter != null) {
                  filters.filter(dedupeFilter);
                }
              }
              versionNormalizationFilter.ifAvailable(filters::filter);
              if (route.getVersioning().isEnabled()) {
                filters.filter(new ApiVersioningGatewayFilter(route.getVersioning()));
              }
              route.getRequestHeaders().forEach(filters::addRequestHeader);
              requestTransformationFactory.ifAvailable(factory -> filters.filter(factory.apply(route.getId())));
              if (route.getSessionAffinity().isEnabled()) {
                filters.filter(new SessionAffinityGatewayFilter(route.getSessionAffinity()));
              }
              if (route.getMaxRequestSize() != null) {
                requestSizeFilterFactory.ifAvailable(factory ->
                    filters.filter(factory.apply(config -> config.setMaxSize(route.getMaxRequestSize()))));
              }
              responseTransformationFactory.ifAvailable(factory -> filters.filter(factory.apply(route.getId())));
              if (resilience.isEnabled()) {
                if (circuitBreakerAvailable) {
                  filters.circuitBreaker(config -> {
                    config.setName(resilience.resolvedCircuitBreakerName(route.getId()));
                    config.setFallbackUri(resilience.resolvedFallbackUri(route.getId()));
                  });
                } else {
                  LOGGER.warn(
                      "Circuit breaker requested for route {} but the Spring Cloud circuit breaker filter factory is unavailable."
                          + " Skipping circuit breaker configuration.",
                      route.getId());
                }

                // Bulkhead filter was removed in newer Gateway releases; circuit breaker provides
                // the primary resilience mechanism. Retain compatibility by skipping bulkhead
                // configuration when the factory is absent.

                GatewayRoutesProperties.ServiceRoute.Resilience.Retry retry = resilience.getRetry();
                if (retry.isEnabled()) {
                  filters.retry(config -> {
                    config.setRetries(Math.max(1, retry.getRetries()));
                    HttpStatus[] statuses = retry.resolvedStatuses();
                    if (statuses.length > 0) {
                      config.setStatuses(statuses);
                    }
                    HttpStatus.Series[] series = retry.resolvedSeries();
                    if (series.length > 0) {
                      config.setSeries(series);
                    }
                    HttpMethod[] retryMethods = retry.resolvedMethods();
                    if (retryMethods.length > 0) {
                      config.setMethods(retryMethods);
                    }
                    Class<? extends Throwable>[] exceptions = retry.resolvedExceptions();
                    if (exceptions.length > 0) {
                      config.setExceptions(exceptions);
                    }
                    GatewayRoutesProperties.ServiceRoute.Resilience.Retry.Backoff backoff = retry.getBackoff();
                    if (backoff.isEnabled()) {
                      config.setBackoff(
                          backoff.getFirstBackoff(),
                          backoff.getMaxBackoff(),
                          backoff.getFactor(),
                          backoff.isBasedOnPreviousValue());
                    }
                  });
                }
              }
              return filters;
            })
            .uri(route.getUri());
      });

      logRouteRegistration(route, resilience);
    }

    return routes.build();
  }

  private GatewayFilter deduplicatePrefixedPathFilter(String prefixPath) {
    if (!StringUtils.hasText(prefixPath)) {
      return null;
    }

    String normalized = prefixPath.trim();
    if (!normalized.startsWith("/")) {
      normalized = '/' + normalized;
    }

    final String prefix = normalized;
    final String doublePrefix = prefix + prefix;

    return (exchange, chain) -> {
      String path = exchange.getRequest().getURI().getRawPath();
      if (!path.startsWith(doublePrefix)) {
        return chain.filter(exchange);
      }

      int index = doublePrefix.length();
      if (path.length() > index && path.charAt(index) != '/') {
        return chain.filter(exchange);
      }

      String adjusted = prefix + path.substring(doublePrefix.length());
      ServerHttpRequest mutatedRequest = exchange.getRequest()
          .mutate()
          .path(adjusted)
          .build();
      return chain.filter(exchange.mutate().request(mutatedRequest).build());
    };
  }

  private void logRouteRegistration(GatewayRoutesProperties.ServiceRoute route,
      GatewayRoutesProperties.ServiceRoute.Resilience resilience) {
    if (!LOGGER.isInfoEnabled()) {
      return;
    }
    String resilienceSummary = (resilience != null && resilience.isEnabled())
        ? resilience.toString()
        : "Resilience{enabled=false}";
    String versionSummary = route.getVersioning().isEnabled()
        ? route.getVersioning().toString()
        : "Versioning{enabled=false}";
    String weightSummary = route.getWeight().isEnabled()
        ? route.getWeight().toString()
        : "Weight{enabled=false}";
    String affinitySummary = route.getSessionAffinity().isEnabled()
        ? route.getSessionAffinity().toString()
        : "SessionAffinity{enabled=false}";
    String lbSummary = route.getLbStrategy().name();
    String prefixSummary = StringUtils.hasText(route.getPrefixPath()) ? route.getPrefixPath() : "/";
    if (route.getMethods().isEmpty()) {
      LOGGER.info(
          "Registered route {} -> {} ({} prefix={}) resilience={} versioning={} weight={} affinity={} lb={}",
          route.getId(),
          route.getUri(),
          route.getPaths(),
          prefixSummary,
          resilienceSummary,
          versionSummary,
          weightSummary,
          affinitySummary,
          lbSummary);
    } else {
      LOGGER.info(
          "Registered route {} -> {} ({}, methods={}, prefix={}) resilience={} versioning={} weight={} affinity={} lb={}",
          route.getId(),
          route.getUri(),
          route.getPaths(),
          route.getMethods(),
          prefixSummary,
          resilienceSummary,
          versionSummary,
          weightSummary,
          affinitySummary,
          lbSummary);
    }
  }
}
