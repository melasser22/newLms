package com.ejada.gateway.fallback;

import com.ejada.gateway.config.GatewayRoutesProperties;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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

  private static final String DEFAULT_MESSAGE = "Downstream service is unavailable. Please retry shortly.";

  private final GatewayRoutesProperties properties;

  public GatewayFallbackController(GatewayRoutesProperties properties) {
    this.properties = properties;
  }

  @RequestMapping("/{routeId}")
  public Mono<ResponseEntity<FallbackResponse>> fallback(@PathVariable String routeId) {
    GatewayRoutesProperties.ServiceRoute.Resilience resilience = properties.findRouteById(routeId)
        .map(GatewayRoutesProperties.ServiceRoute::getResilience)
        .orElse(null);

    HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
    String message = DEFAULT_MESSAGE;
    if (resilience != null) {
      if (resilience.getFallbackStatus() != null) {
        status = resilience.getFallbackStatus();
      }
      message = resilience.resolvedFallbackMessage()
          .filter(StringUtils::hasText)
          .orElse(DEFAULT_MESSAGE);
    }

    FallbackResponse body = new FallbackResponse(routeId, message, Instant.now());
    return Mono.just(ResponseEntity.status(status).body(body));
  }
}
