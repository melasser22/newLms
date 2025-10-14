package com.ejada.gateway.bff;

import static org.mockito.Mockito.when;

import com.ejada.gateway.aggregate.TenantDashboardAggregateResponse;
import com.ejada.gateway.aggregate.TenantDashboardAggregationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class MobileTenantDashboardServiceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Mock
  private TenantDashboardAggregationService aggregationService;

  private MobileTenantDashboardService service;

  @BeforeEach
  void setUp() {
    service = new MobileTenantDashboardService(aggregationService);
  }

  @Test
  void buildTransformsAggregateIntoMobilePayload() throws Exception {
    TenantDashboardAggregateResponse aggregate = new TenantDashboardAggregateResponse(
        json("""{"id":1,"name":"Acme","status":"ACTIVE","healthScore":92}"""),
        json("""{"activeCount":4,"plan":"Enterprise"}"""),
        json("""{"currency":"EUR","monthlySpend":250.42,"usagePercentage":74.3}"""),
        List.of(),
        Instant.parse("2024-01-01T00:00:00Z"));

    when(aggregationService.aggregate(1)).thenReturn(Mono.just(aggregate));

    StepVerifier.create(service.build(1))
        .assertNext(response -> {
          assert response.tenantId().equals("1");
          assert response.tenantName().equals("Acme");
          assert response.plan().equals("Enterprise");
          assert response.billing().currency().equals("EUR");
          assert !response.highlights().isEmpty();
        })
        .verifyComplete();
  }

  private JsonNode json(String value) throws Exception {
    return MAPPER.readTree(value);
  }
}
