package com.ejada.gateway.bff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

class TenantDashboardControllerTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private TenantDashboardService tenantDashboardService;
  private WebTestClient webTestClient;

  @BeforeEach
  void setup() {
    tenantDashboardService = Mockito.mock(TenantDashboardService.class);
    TenantDashboardController controller = new TenantDashboardController(tenantDashboardService);
    webTestClient = WebTestClient.bindToController(controller).build();
  }

  @Test
  void returnsAggregatedDashboard() throws Exception {
    JsonNode tenant = OBJECT_MAPPER.readTree("{\"tenantId\":1}");
    TenantDashboardResponse dashboard = new TenantDashboardResponse(tenant, null, null, null, null, List.of());

    Mockito.when(tenantDashboardService.aggregateDashboard(1, null, null, null))
        .thenReturn(Mono.just(dashboard));

    webTestClient.get()
        .uri("/api/bff/tenants/1/dashboard")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("SUCCESS")
        .jsonPath("$.data.tenant.tenantId").isEqualTo(1);
  }

  @Test
  void propagatesResponseStatusException() {
    Mockito.when(tenantDashboardService.aggregateDashboard(1, null, null, null))
        .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing")));

    webTestClient.get()
        .uri("/api/bff/tenants/1/dashboard")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.status").isEqualTo("ERROR")
        .jsonPath("$.message").isEqualTo("Missing");
  }

  @Test
  void wrapsUnexpectedExceptions() {
    Mockito.when(tenantDashboardService.aggregateDashboard(1, null, null, null))
        .thenReturn(Mono.error(new IllegalStateException("boom")));

    webTestClient.get()
        .uri("/api/bff/tenants/1/dashboard")
        .exchange()
        .expectStatus().isEqualTo(HttpStatus.BAD_GATEWAY)
        .expectBody()
        .jsonPath("$.code").isEqualTo("ERR_TENANT_DASHBOARD");
  }
}
