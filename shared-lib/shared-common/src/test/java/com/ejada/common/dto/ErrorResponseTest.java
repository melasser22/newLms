package com.ejada.common.dto;

import com.ejada.common.context.ContextManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void correlationIdIsAlwaysReturned() {
        ErrorResponse response = ErrorResponse.of("ERR", "boom");

        assertNotNull(response.getCorrelationId());
        assertFalse(response.getCorrelationId().isBlank());
    }

    @Test
    void tenantIdDefaultsFromContext() {
        try (ContextManager.Tenant.Scope ignored = ContextManager.Tenant.openScope("tenant-x")) {
            ErrorResponse response = ErrorResponse.of("ERR", "boom");
            assertEquals("tenant-x", response.getTenantId());
        }
    }
}
