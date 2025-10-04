package com.ejada.gateway.bff;

import com.ejada.gateway.config.GatewayBffProperties;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class TenantDashboardServiceTest {

  private Map<String, ClientResponse> responses;
  private TenantDashboardService service;

  @BeforeEach
  void setUp() {
    responses = new HashMap<>();
    ExchangeFunction exchangeFunction = request -> {
      ClientResponse response = responses.get(request.url().toString());
      if (response == null) {
        return Mono.just(ClientResponse.create(HttpStatus.NOT_FOUND).build());
      }
      return Mono.just(response);
    };

    WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
    GatewayBffProperties properties = new GatewayBffProperties();
    properties.getDashboard().setTenantServiceUri("http://tenant");
    properties.getDashboard().setAnalyticsServiceUri("http://analytics");
    properties.getDashboard().setBillingServiceUri("http://billing");

    service = new TenantDashboardService(builder, properties, (ReactiveCircuitBreakerFactory<?, ?>) null);
  }

  @Test
  void aggregateDashboardReturnsAggregatedPayload() {
    responses.put("http://tenant/api/v1/tenants/1", jsonResponse("""
        {"status":"SUCCESS","code":"SUCCESS-200","message":null,
          "data":{"id":1,"code":"ACME","name":"Acme Corp"}}
        """));

    responses.put("http://analytics/api/v1/analytics/tenants/1/usage-summary?period=MONTHLY",
        jsonResponse("""
            {"tenantId":1,"period":"MONTHLY"}
            """));

    responses.put("http://analytics/api/v1/analytics/tenants/1/feature-adoption",
        jsonResponse("""
            {"tenantId":1,"features":[]}
            """));

    responses.put("http://analytics/api/v1/analytics/tenants/1/cost-forecast",
        jsonResponse("""
            {"tenantId":1,"features":[]}
            """));

    responses.put("http://billing/billing/subscriptions/42/consumption?customerId=99",
        jsonResponse("""
            {"subscriptionId":42,"productConsumptionStts":[]}
            """));

    StepVerifier.create(service.aggregateDashboard(1, 42L, 99L, "monthly"))
        .assertNext(response -> {
          assert response.tenant().get("id").asInt() == 1;
          assert response.usageSummary().get("period").asText().equals("MONTHLY");
          assert response.warnings().isEmpty();
        })
        .verifyComplete();
  }

  @Test
  void aggregateDashboardAddsWarningsWhenDownstreamFails() {
    responses.put("http://tenant/api/v1/tenants/1", jsonResponse("""
        {"status":"SUCCESS","code":"SUCCESS-200","message":null,
          "data":{"id":1,"code":"ACME","name":"Acme Corp"}}
        """));

    responses.put("http://analytics/api/v1/analytics/tenants/1/usage-summary?period=MONTHLY",
        ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build());

    responses.put("http://analytics/api/v1/analytics/tenants/1/feature-adoption",
        ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());

    responses.put("http://analytics/api/v1/analytics/tenants/1/cost-forecast",
        jsonResponse("""
            {"tenantId":1}
            """));

    StepVerifier.create(service.aggregateDashboard(1, null, null, null))
        .assertNext(response -> {
          assert response.tenant() != null;
          assert response.warnings().size() == 2;
          assert response.usageSummary() == null;
          assert response.featureAdoption() == null;
        })
        .verifyComplete();
  }

  @Test
  void aggregateDashboardPropagatesTenantFailure() {
    responses.put("http://tenant/api/v1/tenants/1", ClientResponse.create(HttpStatus.BAD_GATEWAY).build());

    StepVerifier.create(service.aggregateDashboard(1, null, null, null))
        .expectErrorSatisfies(error -> {
          assert error instanceof ResponseStatusException;
          ResponseStatusException ex = (ResponseStatusException) error;
          assert ex.getStatusCode().equals(HttpStatus.BAD_GATEWAY);
        })
        .verify();
  }

  private ClientResponse jsonResponse(String json) {
    return ClientResponse.create(HttpStatus.OK)
        .header("Content-Type", "application/json")
        .body(json)
        .build();
  }
}
