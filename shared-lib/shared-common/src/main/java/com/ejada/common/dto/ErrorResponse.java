package com.ejada.common.dto;

import com.ejada.common.enums.StatusEnums.ApiStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response for Shared APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /** Always ERROR */
    @Builder.Default
    private ApiStatus status = ApiStatus.ERROR;

    /** Business/technical error code (from ErrorCodes) */
    private String code;

    /** Human-readable error message */
    private String message;

    /** Optional detailed errors (e.g., field-level validation issues) */
    private List<String> details;

    /** Tenant ID (multi-tenant awareness) */
    private String tenantId;

    /** Correlation identifier for tracing */
    private String correlationId;

    /** Timestamp of error */
    @Builder.Default
    private Instant timestamp = Instant.now();

    // ===== Static builders =====
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, List<String> details) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .timestamp(Instant.now())
                .build();
    }

    public static ErrorResponse of(String code, String message, List<String> details, String tenantId) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .details(details)
                .tenantId(tenantId)
                .timestamp(Instant.now())
                .build();
    }
}
