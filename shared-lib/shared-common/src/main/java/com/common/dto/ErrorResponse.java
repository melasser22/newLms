package com.common.dto;

import com.common.enums.StatusEnums.ApiStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    /** Trace/Correlation ID (for logs/monitoring) */
    @JsonIgnore
    private String traceId;

    /** Tenant ID (multi-tenant awareness) */
    private String tenantId;

    /** Timestamp of error */
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("correlationId")
    public String getCorrelationId() {
        return traceId;
    }

    // ===== Static builders =====
    public static ErrorResponse of(String code, String message) {
        return ErrorResponse.builder().code(code).message(message).build();
    }

    public static ErrorResponse of(String code, String message, List<String> details, String traceId) {
        return ErrorResponse.builder().code(code).message(message).details(details).traceId(traceId).build();
    }

    public static ErrorResponse of(String code, String message, List<String> details, String traceId, String tenantId) {
        return ErrorResponse.builder().code(code).message(message).details(details).traceId(traceId).tenantId(tenantId).build();
    }
}
