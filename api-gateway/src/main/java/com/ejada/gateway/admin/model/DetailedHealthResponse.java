package com.ejada.gateway.admin.model;

import com.ejada.gateway.config.AdminAggregationProperties;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Aggregated detailed health view emitted by the admin API.
 */
public record DetailedHealthResponse(
    Instant timestamp,
    ComponentHealth redis,
    List<ComponentHealth> downstreamServices,
    List<CircuitBreakerHealth> circuitBreakers,
    boolean ready) {

  public static DetailedHealthResponse compose(ComponentHealth redis,
      List<ComponentHealth> downstream,
      List<CircuitBreakerHealth> breakers) {
    List<ComponentHealth> safeDownstream = downstream == null ? List.of() : List.copyOf(downstream);
    List<CircuitBreakerHealth> safeBreakers = breakers == null ? List.of() : List.copyOf(breakers);
    boolean redisReady = redis != null && redis.isHealthy();
    boolean downstreamReady = safeDownstream.stream()
        .filter(ComponentHealth::required)
        .allMatch(ComponentHealth::isHealthy);
    boolean circuitReady = safeBreakers.stream().noneMatch(CircuitBreakerHealth::isOpen);
    return new DetailedHealthResponse(Instant.now(), redis, safeDownstream, safeBreakers,
        redisReady && downstreamReady && circuitReady);
  }

  public record ComponentHealth(String name, String status, boolean required, long latencyMs, String message) {

    public ComponentHealth {
      name = Objects.requireNonNullElse(name, "unknown");
      status = Objects.requireNonNullElse(status, "UNKNOWN");
      message = Objects.requireNonNullElse(message, "");
    }

    public boolean isHealthy() {
      return "UP".equalsIgnoreCase(status);
    }

    public static ComponentHealth up(String name, boolean required, long latencyMs, String message) {
      return new ComponentHealth(name, "UP", required, latencyMs, message);
    }

    public static ComponentHealth down(String name, boolean required, long latencyMs, String message) {
      return new ComponentHealth(name, "DOWN", required, latencyMs, message);
    }

    public static ComponentHealth fromSnapshot(AdminServiceSnapshot snapshot) {
      if (snapshot == null) {
        return new ComponentHealth("unknown", "UNKNOWN", false, -1, "No snapshot");
      }
      String message = snapshot.details().isEmpty() ? "" : snapshot.details().toString();
      return new ComponentHealth(snapshot.serviceId(), snapshot.status(), snapshot.required(), snapshot.latencyMs(), message);
    }

    public static ComponentHealth unreachable(AdminAggregationProperties.Service service) {
      String name = service.getId() != null ? service.getId() : "unknown";
      return new ComponentHealth(name, "DOWN", service.isRequired(), -1, "Service unreachable");
    }
  }

  public record CircuitBreakerHealth(String serviceName, String state) {

    public CircuitBreakerHealth {
      serviceName = Objects.requireNonNullElse(serviceName, "unknown");
      state = Objects.requireNonNullElse(state, "UNKNOWN").toUpperCase(Locale.ROOT);
    }

    public boolean isOpen() {
      return "OPEN".equalsIgnoreCase(state);
    }
  }
}
