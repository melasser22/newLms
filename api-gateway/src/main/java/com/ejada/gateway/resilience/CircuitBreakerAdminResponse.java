package com.ejada.gateway.resilience;

import java.time.Instant;

/**
 * Response payload for manual circuit breaker operations performed via the admin API.
 */
public record CircuitBreakerAdminResponse(String name, String state, Instant updatedAt) { }
