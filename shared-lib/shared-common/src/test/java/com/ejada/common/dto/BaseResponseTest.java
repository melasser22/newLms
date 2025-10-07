package com.ejada.common.dto;

import com.ejada.common.context.ContextManager;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BaseResponseTest {

    @Test
    void builderCreatesExpectedResponse() {
        Instant now = Instant.now();
        BaseResponse<String> response = BaseResponse.<String>builder()
                .status(ApiStatus.SUCCESS)
                .code("SUCCESS-200")
                .message("ok")
                .data("payload")
                .timestamp(now)
                .build();

        assertEquals(ApiStatus.SUCCESS, response.getStatus());
        assertEquals("SUCCESS-200", response.getCode());
        assertEquals("ok", response.getMessage());
        assertEquals("payload", response.getData());
        assertEquals(now, response.getTimestamp());
        assertTrue(response.isSuccess());
    }

    @Test
    void equalityBasedOnFields() {
        Instant now = Instant.now();
        BaseResponse<String> r1 = BaseResponse.<String>builder()
                .status(ApiStatus.ERROR)
                .code("ERR")
                .message("bad")
                .timestamp(now)
                .build();
        BaseResponse<String> r2 = BaseResponse.<String>builder()
                .status(ApiStatus.ERROR)
                .code("ERR")
                .message("bad")
                .timestamp(now)
                .build();

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void mapTransformsPayloadPreservingMetadata() {
        BaseResponse<String> original = BaseResponse.<String>success("hello");

        BaseResponse<Integer> mapped = original.map(String::length);

        assertEquals(ApiStatus.SUCCESS, mapped.getStatus());
        assertEquals(original.getCode(), mapped.getCode());
        assertEquals(original.getMessage(), mapped.getMessage());
        assertEquals(5, mapped.getData());
    }

    @Test
    void mapHandlesNullPayloadGracefully() {
        BaseResponse<String> original = BaseResponse.<String>success(null);

        BaseResponse<Integer> mapped = original.map(String::length);

        assertNull(mapped.getData());
        assertEquals(original.getCode(), mapped.getCode());
    }

    @Test
    void errorFactoryIncludesDiagnosticPayload() {
        BaseResponse<java.util.Map<String, Object>> response = BaseResponse.error(
                "ERR_TEST",
                "Something went wrong",
                java.util.Map.of("correlationId", "corr-1"));

        assertEquals(ApiStatus.ERROR, response.getStatus());
        assertEquals("ERR_TEST", response.getCode());
        assertEquals("Something went wrong", response.getMessage());
        assertNotNull(response.getData());
        assertEquals("corr-1", response.getData().get("correlationId"));
    }

    @Test
    void tenantIdDefaultsFromContext() {
        try (ContextManager.Tenant.Scope ignore = ContextManager.Tenant.openScope("tenantA")) {
            BaseResponse<Void> r = new BaseResponse<>();
            assertEquals("tenantA", r.getTenantId());
        }
    }
}
