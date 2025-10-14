package com.ejada.gateway.graphql;

import static org.mockito.Mockito.when;

import com.ejada.gateway.aggregate.DownstreamAggregationClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TenantGraphQlServiceTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Mock
  private DownstreamAggregationClient downstreamAggregationClient;

  private TenantGraphQlService service;

  @BeforeEach
  void setUp() {
    service = new TenantGraphQlService(downstreamAggregationClient);
  }

  @Test
  void fetchTenantMapsBasicFields() throws Exception {
    when(downstreamAggregationClient.fetchTenantProfile(1)).thenReturn(Mono.just(json("""
        {"id":1,"code":"ACME","name":"Acme Corp","status":"ACTIVE"}
        """)));

    StepVerifier.create(service.fetchTenant(1))
        .assertNext(tenant -> {
          assert tenant.id() == 1;
          assert tenant.code().equals("ACME");
          assert tenant.name().equals("Acme Corp");
        })
        .verifyComplete();
  }

  @Test
  void fetchSubscriptionsMapsCollection() throws Exception {
    when(downstreamAggregationClient.fetchTenantSubscriptionsBatch(Set.of(1)))
        .thenReturn(Mono.just(Map.of(1, json("""
            {"items":[{"id":"sub-1","product":"CRM","status":"ACTIVE","seats":25}]}
            """))));

    StepVerifier.create(service.fetchSubscriptions(Set.of(1)))
        .assertNext(map -> {
          List<SubscriptionNode> subscriptions = map.get(1);
          assert subscriptions.size() == 1;
          assert subscriptions.get(0).product().equals("CRM");
        })
        .verifyComplete();
  }

  private JsonNode json(String value) throws Exception {
    return MAPPER.readTree(value);
  }
}
