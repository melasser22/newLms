package com.ejada.sec.util;

import com.ejada.common.context.ContextManager;
import com.ejada.common.exception.ValidationException;
import java.util.UUID;

/** Utility helpers for working with the tenant identifier stored in {@link ContextManager}. */
public final class TenantContextResolver {

  private TenantContextResolver() {
    // utility class
  }

  /**
   * Resolve the current tenant identifier from the {@link ContextManager}.
   *
   * @return the parsed tenant identifier
   * @throws ValidationException if the tenant context is missing or malformed
   */
  public static UUID requireTenantId() {
    String raw = ContextManager.Tenant.get();
    if (raw == null || raw.isBlank()) {
      throw new ValidationException("Tenant context is required", "TENANT_CONTEXT_MISSING");
    }
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("Invalid tenant ID format", ex.getMessage());
    }
  }
}

