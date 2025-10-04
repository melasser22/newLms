package com.ejada.gateway.bff;

import com.ejada.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Backend for Frontend (BFF) controller exposing aggregated tenant dashboard
 * endpoints that combine data from multiple downstream services in a single
 * round-trip for web and mobile clients.
 */
@RestController
@RequestMapping("/api/bff/tenants")
public class TenantDashboardController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantDashboardController.class);

  private final TenantDashboardService tenantDashboardService;

  public TenantDashboardController(TenantDashboardService tenantDashboardService) {
    this.tenantDashboardService = tenantDashboardService;
  }

  @GetMapping("/{tenantId}/dashboard")
  public Mono<ResponseEntity<BaseResponse<TenantDashboardResponse>>> getDashboard(
      @PathVariable Integer tenantId,
      @RequestParam(name = "subscriptionId", required = false) Long subscriptionId,
      @RequestParam(name = "customerId", required = false) Long customerId,
      @RequestParam(name = "period", required = false) String period) {
    return tenantDashboardService.aggregateDashboard(tenantId, subscriptionId, customerId, period)
        .map(response -> ResponseEntity.ok(BaseResponse.success("Tenant dashboard aggregated", response)))
        .onErrorResume(ResponseStatusException.class, ex -> Mono.just(ResponseEntity
            .status(ex.getStatusCode())
            .body(BaseResponse.error("ERR_TENANT_DASHBOARD", messageOrDefault(ex.getReason())))))
        .onErrorResume(ex -> {
          LOGGER.warn("Unexpected failure while building tenant dashboard for {}", tenantId, ex);
          return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
              .body(BaseResponse.error("ERR_TENANT_DASHBOARD", "Unable to build tenant dashboard")));
        });
  }

  private String messageOrDefault(String value) {
    return (value == null || value.isBlank()) ? "Unable to build tenant dashboard" : value;
  }
}
