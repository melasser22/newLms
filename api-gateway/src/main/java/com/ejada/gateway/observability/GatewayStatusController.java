package com.ejada.gateway.observability;

import com.ejada.common.dto.BaseResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Exposes a lightweight status document with gateway runtime metadata for dashboards.
 */
@RestController
@RequestMapping("/status")
public class GatewayStatusController {

  private final GatewayStatusService statusService;

  public GatewayStatusController(GatewayStatusService statusService) {
    this.statusService = statusService;
  }

  @GetMapping
  public Mono<BaseResponse<GatewayStatusResponse>> status() {
    return Mono.fromSupplier(statusService::snapshot)
        .map(data -> BaseResponse.success("Gateway status", data));
  }
}
