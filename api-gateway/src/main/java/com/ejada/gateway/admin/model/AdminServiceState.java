package com.ejada.gateway.admin.model;

/**
 * High-level status classification returned from aggregated service health checks.
 */
public enum AdminServiceState {
  UP,
  DOWN,
  DEGRADED,
  UNKNOWN
}
