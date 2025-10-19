package com.ejada.gateway.admin.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Captures the outcome of aggregating a single downstream service's status.
 */
public record AdminServiceSnapshot(
    String serviceId,
    String deployment,
    boolean required,
    AdminServiceState state,
    String status,
    long latencyMs,
    Instant checkedAt,
    Map<String, Object> details) {

  public AdminServiceSnapshot {
    details = details == null ? Map.of() : Map.copyOf(details);
  }

  public static AdminServiceSnapshot success(String serviceId,
      String deployment,
      boolean required,
      Map<String, Object> payload,
      long latencyMs,
      Instant timestamp) {
    String status = extractStatus(payload);
    AdminServiceState state = mapState(status);
    Map<String, Object> details = extractDetails(payload);
    return new AdminServiceSnapshot(serviceId, deployment, required, state, status, latencyMs, timestamp, details);
  }

  public static AdminServiceSnapshot failure(String serviceId,
      String deployment,
      boolean required,
      Throwable failure,
      Instant timestamp) {
    return failure(serviceId,
        deployment,
        required,
        AdminServiceState.DOWN,
        "UNAVAILABLE",
        failure,
        timestamp);
  }

  public static AdminServiceSnapshot failure(String serviceId,
      String deployment,
      boolean required,
      AdminServiceState state,
      String status,
      Throwable failure,
      Instant timestamp) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("error", failure.getClass().getSimpleName());
    details.put("message", Optional.ofNullable(failure.getMessage()).orElse("Unavailable"));
    return new AdminServiceSnapshot(serviceId, deployment, required, state,
        status, -1, timestamp, Collections.unmodifiableMap(details));
  }

  public static AdminServiceSnapshot unauthorized(String serviceId,
      String deployment,
      boolean required,
      WebClientResponseException failure,
      Instant timestamp) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("error", failure.getClass().getSimpleName());
    details.put("status", failure.getStatusCode().value());
    String response = failure.getResponseBodyAsString();
    if (response != null && !response.isBlank()) {
      details.put("message", response.trim());
    } else {
      details.put("message", Optional.ofNullable(failure.getStatusText()).orElse("Unauthorized"));
    }
    return new AdminServiceSnapshot(serviceId, deployment, required, AdminServiceState.UNKNOWN,
        statusCodeName(failure.getStatusCode()), -1, timestamp, Collections.unmodifiableMap(details));
  }

  public static AdminServiceSnapshot httpFailure(String serviceId,
      String deployment,
      boolean required,
      WebClientResponseException failure,
      Instant timestamp) {
    Map<String, Object> details = new LinkedHashMap<>();
    details.put("error", failure.getClass().getSimpleName());
    details.put("status", failure.getStatusCode().value());
    String response = failure.getResponseBodyAsString();
    if (response != null && !response.isBlank()) {
      details.put("message", response.trim());
    } else {
      details.put("message", Optional.ofNullable(failure.getMessage()).orElse("Unavailable"));
    }
    return new AdminServiceSnapshot(serviceId, deployment, required, AdminServiceState.DOWN,
        statusCodeName(failure.getStatusCode()), -1, timestamp, Collections.unmodifiableMap(details));
  }

  private static String extractStatus(Map<String, Object> payload) {
    if (payload == null) {
      return "UNKNOWN";
    }
    Object candidate = payload.get("status");
    return Objects.toString(candidate, "UNKNOWN").toUpperCase(Locale.ROOT);
  }

  private static Map<String, Object> extractDetails(Map<String, Object> payload) {
    if (payload == null) {
      return Collections.emptyMap();
    }
    Object details = payload.get("details");
    if (details instanceof Map<?, ?> map) {
      Map<String, Object> sanitized = new LinkedHashMap<>();
      map.forEach((key, value) -> sanitized.put(Objects.toString(key, ""), value));
      return Collections.unmodifiableMap(sanitized);
    }
    Map<String, Object> single = new LinkedHashMap<>();
    if (details != null) {
      single.put("details", details);
    }
    return Collections.unmodifiableMap(single);
  }

  private static AdminServiceState mapState(String status) {
    if (!String.valueOf(status).isEmpty()) {
      return switch (status.toUpperCase(Locale.ROOT)) {
        case "UP" -> AdminServiceState.UP;
        case "OUT_OF_SERVICE", "DOWN" -> AdminServiceState.DOWN;
        case "DEGRADED" -> AdminServiceState.DEGRADED;
        default -> AdminServiceState.UNKNOWN;
      };
    }
    return AdminServiceState.UNKNOWN;
  }

  private static String statusCodeName(HttpStatusCode statusCode) {
    if (statusCode instanceof HttpStatus httpStatus) {
      return httpStatus.name();
    }
    return statusCode.toString();
  }
}
