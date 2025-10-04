package com.ejada.gateway.bff;

import com.ejada.common.dto.BaseResponse;
import com.ejada.gateway.config.GatewayBffProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Aggregates data from multiple downstream services (tenant, analytics,
 * billing) to provide a cohesive tenant dashboard response for UI clients.
 */
@Service
public class TenantDashboardService {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantDashboardService.class);
  private static final ParameterizedTypeReference<BaseResponse<JsonNode>> TENANT_RESPONSE_TYPE =
      new ParameterizedTypeReference<>() {
      };

  private final WebClient tenantClient;
  private final WebClient analyticsClient;
  private final WebClient billingClient;
  private final GatewayBffProperties.TenantDashboardProperties properties;
  private final ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory;

  @Autowired
  public TenantDashboardService(WebClient.Builder webClientBuilder,
      GatewayBffProperties properties,
      ObjectProvider<ReactiveCircuitBreakerFactory<?, ?>> circuitBreakerFactoryProvider) {
    this(webClientBuilder, properties, circuitBreakerFactoryProvider.getIfAvailable());
  }

  TenantDashboardService(WebClient.Builder webClientBuilder,
      GatewayBffProperties properties,
      ReactiveCircuitBreakerFactory<?, ?> circuitBreakerFactory) {
    Objects.requireNonNull(webClientBuilder, "webClientBuilder");
    this.properties = Objects.requireNonNull(properties, "properties").getDashboard();
    this.tenantClient = webClientBuilder.clone().baseUrl(this.properties.getTenantServiceUri()).build();
    this.analyticsClient = webClientBuilder.clone().baseUrl(this.properties.getAnalyticsServiceUri()).build();
    this.billingClient = webClientBuilder.clone().baseUrl(this.properties.getBillingServiceUri()).build();
    this.circuitBreakerFactory = circuitBreakerFactory;
  }

  /**
   * Compose a tenant dashboard payload by fan-out/fan-in calls to downstream services.
   */
  public Mono<TenantDashboardResponse> aggregateDashboard(Integer tenantId,
      Long subscriptionId,
      Long customerId,
      String requestedPeriod) {
    Objects.requireNonNull(tenantId, "tenantId");

    String period = sanitisePeriod(requestedPeriod);

    Mono<JsonNode> tenantMono = withCircuitBreaker("bff-tenant-profile", fetchTenantProfile(tenantId));

    Mono<SafeResult<JsonNode>> usageMono = safeFetch("bff-analytics-usage",
        fetchUsageSummary(tenantId, period),
        "Usage summary unavailable");

    Mono<SafeResult<JsonNode>> adoptionMono = safeFetch("bff-analytics-adoption",
        fetchFeatureAdoption(tenantId),
        "Feature adoption analytics unavailable");

    Mono<SafeResult<JsonNode>> costMono = safeFetch("bff-analytics-cost",
        fetchCostForecast(tenantId),
        "Cost forecast analytics unavailable");

    Mono<SafeResult<JsonNode>> consumptionMono;
    if (subscriptionId != null) {
      consumptionMono = safeFetch("bff-billing-consumption",
          fetchConsumptionSnapshot(subscriptionId, customerId),
          "Billing consumption snapshot unavailable");
    } else {
      consumptionMono = Mono.just(SafeResult.success(null));
    }

    return Mono.zip(tenantMono, usageMono, adoptionMono, costMono, consumptionMono)
        .map(tuple -> {
          JsonNode tenant = tuple.getT1();
          SafeResult<JsonNode> usage = tuple.getT2();
          SafeResult<JsonNode> adoption = tuple.getT3();
          SafeResult<JsonNode> cost = tuple.getT4();
          SafeResult<JsonNode> consumption = tuple.getT5();

          List<String> warnings = Stream.of(usage.warning(), adoption.warning(), cost.warning(), consumption.warning())
              .filter(StringUtils::hasText)
              .distinct()
              .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

          return new TenantDashboardResponse(tenant,
              usage.value(),
              adoption.value(),
              cost.value(),
              consumption.value(),
              warnings);
        })
        .onErrorResume(ResponseStatusException.class, Mono::error)
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to aggregate dashboard for tenant {}", tenantId, ex);
          return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
              "Unable to aggregate tenant dashboard", ex));
        });
  }

  private Mono<JsonNode> fetchTenantProfile(Integer tenantId) {
    return tenantClient.get()
        .uri("/api/v1/tenants/{tenantId}", tenantId)
        .retrieve()
        .bodyToMono(TENANT_RESPONSE_TYPE)
        .flatMap(response -> {
          if (response == null || !response.isSuccess() || response.getData() == null) {
            LOGGER.warn("Tenant service returned unexpected payload for tenant {}", tenantId);
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Tenant profile unavailable"));
          }
          return Mono.just(response.getData());
        });
  }

  private Mono<JsonNode> fetchUsageSummary(Integer tenantId, String period) {
    return analyticsClient.get()
        .uri(builder -> builder.path("/api/v1/analytics/tenants/{tenantId}/usage-summary")
            .queryParam("period", period)
            .build(tenantId))
        .retrieve()
        .bodyToMono(JsonNode.class);
  }

  private Mono<JsonNode> fetchFeatureAdoption(Integer tenantId) {
    return analyticsClient.get()
        .uri("/api/v1/analytics/tenants/{tenantId}/feature-adoption", tenantId)
        .retrieve()
        .bodyToMono(JsonNode.class);
  }

  private Mono<JsonNode> fetchCostForecast(Integer tenantId) {
    return analyticsClient.get()
        .uri("/api/v1/analytics/tenants/{tenantId}/cost-forecast", tenantId)
        .retrieve()
        .bodyToMono(JsonNode.class);
  }

  private Mono<JsonNode> fetchConsumptionSnapshot(Long subscriptionId, Long customerId) {
    return billingClient.get()
        .uri(builder -> {
          var uriBuilder = builder.path("/billing/subscriptions/{subscriptionId}/consumption");
          if (customerId != null) {
            uriBuilder.queryParam("customerId", customerId);
          }
          URI uri = uriBuilder.build(subscriptionId);
          return uri;
        })
        .retrieve()
        .bodyToMono(JsonNode.class);
  }

  private String sanitisePeriod(String requestedPeriod) {
    if (StringUtils.hasText(requestedPeriod)) {
      return requestedPeriod.trim().toUpperCase(Locale.ROOT);
    }
    return properties.getDefaultPeriod();
  }

  private <T> Mono<T> withCircuitBreaker(String name, Mono<T> toRun) {
    if (circuitBreakerFactory == null) {
      return toRun;
    }
    ReactiveCircuitBreaker breaker = circuitBreakerFactory.create(name);
    return Mono.defer(() -> breaker.run(toRun, throwable -> Mono.error(throwable)));
  }

  private <T> Mono<SafeResult<T>> safeFetch(String circuitBreakerName, Mono<T> call, String warning) {
    return withCircuitBreaker(circuitBreakerName, call)
        .map(SafeResult::success)
        .switchIfEmpty(Mono.defer(() -> {
          LOGGER.debug("{} returned empty payload", circuitBreakerName);
          return Mono.just(SafeResult.warning(warning));
        }))
        .onErrorResume(ex -> {
          LOGGER.warn("{} failed: {}", circuitBreakerName, ex.getMessage());
          return Mono.just(SafeResult.warning(warning));
        });
  }

  private record SafeResult<T>(T value, String warning) {

    static <T> SafeResult<T> success(T value) {
      return new SafeResult<>(value, null);
    }

    static <T> SafeResult<T> warning(String warning) {
      return new SafeResult<>(null, warning);
    }
  }
}
