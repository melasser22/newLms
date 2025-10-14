package com.ejada.gateway.bff;

import com.ejada.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Backend-for-Frontend controller that exposes the mobile optimised dashboard aggregation.
 */
@RestController
@RequestMapping("/api/bff/mobile/tenants")
public class MobileTenantDashboardController {

  private static final Logger LOGGER = LoggerFactory.getLogger(MobileTenantDashboardController.class);

  private final MobileTenantDashboardService mobileTenantDashboardService;

  public MobileTenantDashboardController(MobileTenantDashboardService mobileTenantDashboardService) {
    this.mobileTenantDashboardService = mobileTenantDashboardService;
  }

  @GetMapping("/{tenantId}/dashboard")
  public Mono<ResponseEntity<BaseResponse<MobileTenantDashboardResponse>>> dashboard(@PathVariable Integer tenantId) {
    return mobileTenantDashboardService.build(tenantId)
        .map(payload -> ResponseEntity.ok(BaseResponse.success("Mobile tenant dashboard", payload)))
        .onErrorResume(ResponseStatusException.class, ex -> Mono.just(ResponseEntity
            .status(ex.getStatusCode())
            .body(BaseResponse.error("ERR_MOBILE_DASHBOARD", messageOrDefault(ex.getReason())))))
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to build mobile dashboard for tenant {}", tenantId, ex);
          return Mono.just(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
              .body(BaseResponse.error("ERR_MOBILE_DASHBOARD", "Unable to build mobile tenant dashboard")));
        });
  }

  private String messageOrDefault(String value) {
    return (value == null || value.isBlank()) ? "Unable to build mobile tenant dashboard" : value;
  }
}
