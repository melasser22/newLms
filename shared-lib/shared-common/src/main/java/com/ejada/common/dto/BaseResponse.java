package com.ejada.common.dto;

import com.ejada.common.context.ContextManager;
import com.ejada.common.enums.StatusEnums.ApiStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard response wrapper for all Shared APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    /** High-level status: SUCCESS, ERROR, WARNING */
    private ApiStatus status;

    /**
     * Business/technical code (e.g., ERR-VALIDATION, ERR-NOT-FOUND, SUCCESS-200)
     */
    private String code;

    /** Human-readable message (may be null) */
    @Nullable
    private String message;

    /** Optional payload (generic type for flexibility) */
    @Nullable
    private T data;

    /** Timestamp of response */
    @Builder.Default
    private Instant timestamp = Instant.now();
    /** Tenant identifier for multi-tenancy */
    private String tenantId;

    @JsonProperty("tenantId")
    public String getTenantId() {
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = ContextManager.Tenant.get();
        }
        return tenantId;
    }

    // ===== Static builders (nice usability) =====

    public static <T> BaseResponse<T> success(T data) {
        return build(ApiStatus.SUCCESS, "SUCCESS-200", "Operation successful", data);
    }

    /**
     * Create a success response with a custom message.
     *
     * @param message human-readable success message
     * @param data    optional payload
     * @return a new BaseResponse with status SUCCESS
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return build(ApiStatus.SUCCESS, "SUCCESS-200", message, data);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return build(ApiStatus.ERROR, code, message, null);
    }

    /**
     * Create an error response with additional diagnostic payload.
     *
     * @param code    business or technical error code
     * @param message human-readable description of the error
     * @param data    optional payload containing structured diagnostic details
     * @param <T>     type of the diagnostic payload
     * @return a new BaseResponse with status ERROR
     */
    public static <T> BaseResponse<T> error(String code, String message, T data) {
        return build(ApiStatus.ERROR, code, message, data);
    }

    public static <T> BaseResponse<T> warning(String code, String message, T data) {
        return build(ApiStatus.WARNING, code, message, data);
    }

    private static <T> BaseResponse<T> build(ApiStatus status, String code, String message, T data) {
        return BaseResponse.<T>builder()
                .status(status)
                .code(code)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Convenience check for SUCCESS responses.
     *
     * @return true if status is ApiStatus.SUCCESS
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isSuccess() {
        return status == ApiStatus.SUCCESS;
    }

    /**
     * Convenience check for ERROR responses.
     *
     * @return true if status is ApiStatus.ERROR
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isError() {
        return status == ApiStatus.ERROR;
    }

    /**
     * Convenience check for WARNING responses.
     *
     * @return true if status is ApiStatus.WARNING
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isWarning() {
        return status == ApiStatus.WARNING;
    }

    /**
     * Transform the payload while preserving status, code and message metadata.
     *
     * <p>If the payload is {@code null}, the mapper is not invoked and {@code null}
     * is returned as the new payload.
     *
     * @param mapper function to transform the existing payload
     * @param <R>    target type of the new payload
     * @return a new {@link BaseResponse} instance with mapped data
     */
    public <R> BaseResponse<R> map(Function<? super T, ? extends R> mapper) {
        R newData = (data != null && mapper != null) ? mapper.apply(data) : null;
        return BaseResponse.<R>builder()
                .status(status)
                .code(code)
                .message(message)
                .data(newData)
                .timestamp(timestamp)
                .tenantId(getTenantId())
                .build();
    }

}

