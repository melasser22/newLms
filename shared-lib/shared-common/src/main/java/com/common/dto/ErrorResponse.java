package com.common.dto;

import com.common.enums.StatusEnums.ApiStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

/**
 * Standardized error response for Shared APIs.
 */
public class ErrorResponse {

    /** Always ERROR */
    private ApiStatus status = ApiStatus.ERROR;

    /** Business/technical error code (from ErrorCodes) */
    private String code;

    /** Human-readable error message */
    private String message;

    /** Optional detailed errors (e.g., field-level validation issues) */
    private List<String> details;

    /** Trace/Correlation ID (for logs/monitoring) */
    private String traceId;

    /** Tenant ID (multi-tenant awareness) */
    private String tenantId;

    /** Timestamp of error */
    private Instant timestamp;

    // ===== Constructors =====
    public ErrorResponse() {
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String code, String message, List<String> details, String traceId) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }

    public ErrorResponse(String code, String message, List<String> details, String traceId, String tenantId) {
        this.code = code;
        this.message = message;
        this.details = details;
        this.traceId = traceId;
        this.tenantId = tenantId;
        this.timestamp = Instant.now();
    }

    // ===== Static builders =====
    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message, null, null);
    }

    public static ErrorResponse of(String code, String message, List<String> details, String traceId) {
        return new ErrorResponse(code, message, details, traceId);
    }

    public static ErrorResponse of(String code, String message, List<String> details, String traceId, String tenantId) {
        return new ErrorResponse(code, message, details, traceId, tenantId);
    }

    // ===== Getters & Setters =====
    public ApiStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }
    
    @JsonIgnore
    public String getTraceId() {
        return traceId;
    }

    @JsonProperty("correlationId")
    public String getCorrelationId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
