package com.ejada.gateway.admin.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated health payload for admin detailed health endpoint.
 */
public record DetailedHealthStatus(
    RedisHealthStatus redis,
    @JsonProperty("downstream-services") List<AdminServiceSnapshot> downstreamServices,
    @JsonProperty("circuit-breakers") List<CircuitBreakerHealth> circuitBreakers) {

  public DetailedHealthStatus {
    downstreamServices = downstreamServices == null ? List.of() : List.copyOf(downstreamServices);
    circuitBreakers = circuitBreakers == null ? List.of() : List.copyOf(circuitBreakers);
  }

  public static DetailedHealthStatus empty() {
    return new DetailedHealthStatus(RedisHealthStatus.unavailable(), Collections.emptyList(), Collections.emptyList());
  }

  public record CircuitBreakerHealth(String serviceName, String state) {
    public CircuitBreakerHealth {
      Objects.requireNonNull(serviceName, "serviceName");
      Objects.requireNonNull(state, "state");
    }
  }

  public record RedisHealthStatus(String status, long latencyMs, String message) {
    public static RedisHealthStatus unavailable() {
      return new RedisHealthStatus("DOWN", -1, "Not assessed");
    }

    public boolean isUp() {
      return "UP".equalsIgnoreCase(status);
    }
  }
}
