package com.common.dto;

import com.common.enums.StatusEnums.ApiStatus;
import jakarta.annotation.Nullable;
import java.time.Instant;

/**
 * Standard response wrapper for all Shared APIs.
 */
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
    private Instant timestamp;

    // ===== Constructors =====
    public BaseResponse() {
        this.timestamp = Instant.now();
    }

    public BaseResponse(ApiStatus status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = Instant.now();
    }

    // ===== Static builders (nice usability) =====
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(ApiStatus.SUCCESS, "SUCCESS-200", "Operation successful", data);
    }

    /**
     * Create a success response with a custom message.
     *
     * @param message human-readable success message
     * @param data    optional payload
     * @return a new BaseResponse with status SUCCESS
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return new BaseResponse<>(ApiStatus.SUCCESS, "SUCCESS-200", message, data);
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return new BaseResponse<>(ApiStatus.ERROR, code, message, null);
    }

    public static <T> BaseResponse<T> warning(String code, String message, T data) {
        return new BaseResponse<>(ApiStatus.WARNING, code, message, data);
    }

    // ===== Getters & Setters =====
    public ApiStatus getStatus() {
        return status;
    }

    public void setStatus(ApiStatus status) {
        this.status = status;
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

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
