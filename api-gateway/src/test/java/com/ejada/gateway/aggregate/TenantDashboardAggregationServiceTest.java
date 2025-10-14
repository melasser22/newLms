package com.ejada.gateway.aggregate;

import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TenantDashboardAggregationServiceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Mock
  private DownstreamAggregationClient downstreamAggregationClient;

  private TenantDashboardAggregationService service;

  @BeforeEach
  void setUp() {
    service = new TenantDashboardAggregationService(downstreamAggregationClient);
  }

  @Test
  void aggregateReturnsDashboardWhenAllServicesRespond() throws Exception {
    when(downstreamAggregationClient.fetchTenantProfile(1)).thenReturn(Mono.just(json("""
        {"id":1,"code":"ACME","name":"Acme","status":"ACTIVE"}
        """)));
    when(downstreamAggregationClient.fetchTenantSubscriptions(1)).thenReturn(Mono.just(json("""
        {"items":[{"id":"sub-1","status":"ACTIVE"}]}
        """)));
    when(downstreamAggregationClient.fetchBillingSummary(1)).thenReturn(Mono.just(json("""
        {"currency":"USD","monthlySpend":120.5}
        """)));

    StepVerifier.create(service.aggregate(1))
        .assertNext(result -> {
          assert result.tenant().get("id").asInt() == 1;
          assert result.subscriptions().get("items").isArray();
          assert result.billing().get("monthlySpend").asDouble() == 120.5;
          assert result.warnings().isEmpty();
        })
        .verifyComplete();
  }

  @Test
  void aggregateAddsWarningsWhenOptionalServicesFail() throws Exception {
    when(downstreamAggregationClient.fetchTenantProfile(1)).thenReturn(Mono.just(json("""
        {"id":1,"code":"ACME","name":"Acme"}
        """)));
    when(downstreamAggregationClient.fetchTenantSubscriptions(1))
        .thenReturn(Mono.error(new IllegalStateException("subscriptions down")));
    when(downstreamAggregationClient.fetchBillingSummary(1))
        .thenReturn(Mono.error(new IllegalStateException("billing down")));

    StepVerifier.create(service.aggregate(1))
        .assertNext(result -> {
          assert result.tenant().get("id").asInt() == 1;
          assert result.subscriptions() == null;
          assert result.billing() == null;
          assert result.warnings().size() == 2;
        })
        .verifyComplete();
  }

  @Test
  void aggregatePropagatesTenantFailure() {
    when(downstreamAggregationClient.fetchTenantProfile(1))
        .thenReturn(Mono.error(new IllegalStateException("tenant down")));

    StepVerifier.create(service.aggregate(1))
        .expectError(ResponseStatusException.class)
        .verify();
  }

  private JsonNode json(String value) throws Exception {
    return MAPPER.readTree(value);
  }
}
