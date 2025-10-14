package com.ejada.gateway.aggregate;

import com.ejada.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * REST entrypoint exposing the aggregated tenant dashboard view backed by fan-out queries to the
 * tenant, subscription and billing services. Designed for lightweight consumers that do not need
 * the richer analytics payload returned by the BFF endpoints.
 */
@RestController
@RequestMapping(path = "/api/aggregate", produces = MediaType.APPLICATION_JSON_VALUE)
public class TenantDashboardAggregationController {

  private static final Logger LOGGER = LoggerFactory.getLogger(TenantDashboardAggregationController.class);

  private final TenantDashboardAggregationService aggregationService;

  public TenantDashboardAggregationController(TenantDashboardAggregationService aggregationService) {
    this.aggregationService = aggregationService;
  }

  @GetMapping("/dashboard/{tenantId}")
  public Mono<ResponseEntity<BaseResponse<TenantDashboardAggregateResponse>>> aggregateDashboard(
      @PathVariable Integer tenantId) {
    return aggregationService.aggregate(tenantId)
        .map(payload -> ResponseEntity.ok(BaseResponse.success("Tenant dashboard aggregated", payload)))
        .onErrorResume(ResponseStatusException.class, ex -> Mono.just(ResponseEntity
            .status(ex.getStatusCode())
            .body(BaseResponse.error("ERR_DASHBOARD_AGGREGATE", messageOrDefault(ex.getReason())))))
        .onErrorResume(ex -> {
          LOGGER.warn("Unexpected dashboard aggregation failure for tenant {}", tenantId, ex);
          return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
              .body(BaseResponse.error("ERR_DASHBOARD_AGGREGATE", "Unable to aggregate tenant dashboard")));
        });
  }

  private String messageOrDefault(String value) {
    return (value == null || value.isBlank()) ? "Unable to aggregate tenant dashboard" : value;
  }
}
