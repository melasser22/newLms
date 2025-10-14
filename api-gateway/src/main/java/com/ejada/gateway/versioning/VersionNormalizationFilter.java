package com.ejada.gateway.versioning;

import com.ejada.common.constants.HeaderNames;
import com.ejada.gateway.context.GatewayRequestAttributes;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Gateway filter responsible for applying the {@link VersionMappingResolver} result to incoming
 * requests. It rewrites legacy paths, enriches request/response headers and propagates metadata for
 * downstream services and observability.
 */
public class VersionNormalizationFilter implements GatewayFilter, Ordered {

  private static final Logger LOGGER = LoggerFactory.getLogger(VersionNormalizationFilter.class);

  private final VersionMappingResolver resolver;
  private final VersionAnalyticsService analyticsService;
  private final com.ejada.gateway.versioning.preference.TenantVersionPreferenceService preferenceService;

  public VersionNormalizationFilter(VersionMappingResolver resolver) {
    this(resolver, null, null);
  }

  public VersionNormalizationFilter(VersionMappingResolver resolver,
      VersionAnalyticsService analyticsService,
      com.ejada.gateway.versioning.preference.TenantVersionPreferenceService preferenceService) {
    this.resolver = resolver;
    this.analyticsService = analyticsService;
    this.preferenceService = preferenceService;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    if (!resolver.isEnabled()) {
      return chain.filter(exchange);
    }

    Mono<ServerWebExchange> prepared = (preferenceService != null)
        ? preferenceService.resolvePreferredVersion(exchange)
            .doOnNext(optional -> optional.ifPresent(version -> exchange.getAttributes()
                .put(GatewayRequestAttributes.API_VERSION_PREFERENCE, version)))
            .thenReturn(exchange)
        : Mono.just(exchange);

    return prepared.flatMap(currentExchange -> {
      Optional<VersionMappingResult> maybeResult = resolver.resolve(currentExchange);
      if (maybeResult.isEmpty()) {
        return chain.filter(currentExchange);
      }

      VersionMappingResult result = maybeResult.get();
      if (result.isUnsupported()) {
        LOGGER.debug("Rejecting unsupported API version {} for mapping {}", result.getRequestedVersion(), result.getMappingId());
        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Requested API version is not supported"));
      }

      ServerHttpRequest request = currentExchange.getRequest();
      ServerHttpRequest.Builder builder = request.mutate();

      String rewrittenPath = result.getRewrittenPath();
      if (StringUtils.hasText(rewrittenPath)) {
        URI updated = UriComponentsBuilder.fromUri(request.getURI())
            .replacePath(rewrittenPath)
            .build(true)
            .toUri();
        builder.uri(updated);
      }

      if (StringUtils.hasText(result.getResolvedVersion())) {
        builder.headers(httpHeaders -> httpHeaders.set(HeaderNames.API_VERSION, result.getResolvedVersion()));
      }

      if (StringUtils.hasText(result.getResolutionSource())) {
        builder.headers(httpHeaders -> httpHeaders.set("X-Api-Version-Source", result.getResolutionSource()));
      }

      Map<String, String> additionalHeaders = result.getAdditionalHeaders();
      if (!CollectionUtils.isEmpty(additionalHeaders)) {
        builder.headers(httpHeaders -> additionalHeaders.forEach(httpHeaders::set));
      }

      ServerWebExchange mutated = currentExchange.mutate().request(builder.build()).build();
      mutated.getAttributes().put(GatewayRequestAttributes.API_VERSION, result.getResolvedVersion());
      mutated.getAttributes().put(GatewayRequestAttributes.API_VERSION_REQUESTED, result.getRequestedVersion());
      mutated.getAttributes().put(GatewayRequestAttributes.API_VERSION_SOURCE, result.getResolutionSource());

      if (analyticsService != null) {
        analyticsService.record(mutated, result);
      }

      registerCompatibilityTransformation(mutated, result);

      if (result.isDeprecated()) {
        mutated.getResponse().beforeCommit(() -> {
          mutated.getResponse().getHeaders().set("Deprecation",
              StringUtils.hasText(result.getSunset()) ? result.getSunset() : "true");
          if (StringUtils.hasText(result.getWarning())) {
            mutated.getResponse().getHeaders().add("X-API-Deprecation", result.getWarning());
          }
          if (StringUtils.hasText(result.getSunset())) {
            mutated.getResponse().getHeaders().set("Sunset", result.getSunset());
          }
          if (StringUtils.hasText(result.getPolicyLink())) {
            mutated.getResponse().getHeaders().add("Link", '<' + result.getPolicyLink() + ">; rel=\"deprecation\"");
          }
          if (!result.getCompatibility().isEmpty()) {
            mutated.getResponse().getHeaders().set("X-Api-Compatible-With", String.join(",", result.getCompatibility()));
          }
          return Mono.empty();
        });
      } else if (!result.getCompatibility().isEmpty()) {
        mutated.getResponse().beforeCommit(() -> {
          mutated.getResponse().getHeaders().set("X-Api-Compatible-With", String.join(",", result.getCompatibility()));
          return Mono.empty();
        });
      }

      return chain.filter(mutated);
    });
  }

  @Override
  public int getOrder() {
    // Run early so route-specific filters can observe the normalised path and version metadata.
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }

  private void registerCompatibilityTransformation(ServerWebExchange exchange, VersionMappingResult result) {
    String requested = result.getRequestedVersion();
    String resolved = result.getResolvedVersion();
    if (!StringUtils.hasText(requested) || !StringUtils.hasText(resolved)) {
      return;
    }
    if (resolved.equals(requested)) {
      return;
    }
    String routeId = result.getCompatibilityTransformations().get(requested);
    if (!StringUtils.hasText(routeId) && result.getCompatibility().contains(requested)) {
      routeId = "compatibility." + resolved + '.' + requested;
    }
    if (StringUtils.hasText(routeId)) {
      exchange.getAttributes().put(GatewayRequestAttributes.API_VERSION_TRANSFORMATION_ROUTE, routeId);
    }
  }
}
