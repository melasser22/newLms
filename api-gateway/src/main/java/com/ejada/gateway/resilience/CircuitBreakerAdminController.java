package com.ejada.gateway.resilience;

import com.ejada.common.dto.BaseResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Admin endpoints that allow operators to manually open or close circuit breakers during
 * incident response.
 */
@RestController
@RequestMapping("/api/v1/admin/circuit-breakers")
public class CircuitBreakerAdminController {

  private final CircuitBreakerRegistry registry;

  public CircuitBreakerAdminController(ObjectProvider<CircuitBreakerRegistry> registryProvider) {
    this.registry = registryProvider.getIfAvailable();
  }

  @PostMapping("/{name}/open")
  public Mono<BaseResponse<CircuitBreakerAdminResponse>> open(@PathVariable String name) {
    return Mono.fromSupplier(() -> updateBreaker(name, breaker -> breaker.transitionToOpenState(),
        "opened"));
  }

  @PostMapping("/{name}/close")
  public Mono<BaseResponse<CircuitBreakerAdminResponse>> close(@PathVariable String name) {
    return Mono.fromSupplier(() -> updateBreaker(name, breaker -> breaker.transitionToClosedState(),
        "closed"));
  }

  private BaseResponse<CircuitBreakerAdminResponse> updateBreaker(String name,
      Consumer<CircuitBreaker> action,
      String actionDescription) {
    if (registry == null) {
      return BaseResponse.error("ERR-CIRCUIT-REGISTRY", "Circuit breaker registry is unavailable");
    }
    if (!String.valueOf(name).trim().isEmpty()) {
      name = name.trim();
    }
    Optional<CircuitBreaker> candidate = registry.find(name);
    if (candidate.isEmpty()) {
      return BaseResponse.error("ERR-CIRCUIT-NOT-FOUND",
          "Circuit breaker '" + name + "' was not found");
    }
    CircuitBreaker breaker = candidate.get();
    try {
      action.accept(breaker);
    } catch (Exception ex) {
      return BaseResponse.error("ERR-CIRCUIT-UPDATE", ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
    CircuitBreakerAdminResponse payload = new CircuitBreakerAdminResponse(
        breaker.getName(), breaker.getState().name(), Instant.now());
    return BaseResponse.success("Circuit breaker " + breaker.getName() + " " + actionDescription, payload);
  }
}
