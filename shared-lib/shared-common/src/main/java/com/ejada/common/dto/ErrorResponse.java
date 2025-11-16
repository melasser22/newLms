package com.ejada.common.dto;

import com.ejada.common.context.ContextManager;
import com.ejada.common.context.CorrelationContextUtil;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @Builder.Default
    private String correlationId = CorrelationContextUtil.getCorrelationId();

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

    @JsonProperty("tenantId")
    public String getTenantId() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = ContextManager.Tenant.get();
        }
        return tenantId;
    }

    @JsonProperty("correlationId")
    public String getCorrelationId() {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = CorrelationContextUtil.getCorrelationId();
        }
        return correlationId;
    }
}
