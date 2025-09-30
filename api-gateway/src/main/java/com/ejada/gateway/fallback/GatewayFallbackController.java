package com.ejada.gateway.fallback;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Reactive handler that provides graceful degradation responses when
 * downstream services are temporarily unavailable and the circuit breaker
 * triggers.
 */
@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

  @RequestMapping("/{routeId}")
  public Mono<ResponseEntity<FallbackResponse>> fallback(@PathVariable String routeId) {
    FallbackResponse body =
        new FallbackResponse(
            routeId,
            "Downstream service is unavailable. Please retry shortly.",
            Instant.now());
    return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body));
  }
}
