package com.common.dto;

import com.common.enums.StatusEnums.ApiStatus;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseResponse<?> that = (BaseResponse<?>) o;
        return status == that.status &&
                Objects.equals(code, that.code) &&
                Objects.equals(message, that.message) &&
                Objects.equals(data, that.data) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, code, message, data, timestamp);
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "status=" + status +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }

    /**
     * Convenience check for SUCCESS responses.
     *
     * @return true if status is ApiStatus.SUCCESS
     */
    public boolean isSuccess() {
        return status == ApiStatus.SUCCESS;
    }

    /**
     * Convenience check for ERROR responses.
     *
     * @return true if status is ApiStatus.ERROR
     */
    public boolean isError() {
        return status == ApiStatus.ERROR;
    }

    /**
     * Convenience check for WARNING responses.
     *
     * @return true if status is ApiStatus.WARNING
     */
    public boolean isWarning() {
        return status == ApiStatus.WARNING;
    }

    /**
     * Transform the payload while preserving status, code and message metadata.
<<<<<<< HEAD
=======
     * <p>
     * If the payload is {@code null}, the mapper is not invoked and {@code null}
>>>>>>> fc5fe58 (chore: register config module in parent)
     * is returned as the new payload.
     *
     * @param mapper function to transform the existing payload
     * @param <R>    target type of the new payload
     * @return a new {@link BaseResponse} instance with mapped data
     */
    public <R> BaseResponse<R> map(Function<? super T, ? extends R> mapper) {
        R newData = (data != null && mapper != null) ? mapper.apply(data) : null;
        return new BaseResponse<>(status, code, message, newData);
    }

    /**
     * Create a builder for {@link BaseResponse}.
     *
     * @param <T> payload type
     * @return new Builder instance
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder for {@link BaseResponse} allowing fine-grained construction.
     */
    public static final class Builder<T> {
        private ApiStatus status;
        private String code;
        private String message;
        private T data;
        private Instant timestamp = Instant.now();

        private Builder() {}

        public Builder<T> status(ApiStatus status) {
            this.status = status;
            return this;
        }

        public Builder<T> code(String code) {
            this.code = code;
            return this;
        }

        public Builder<T> message(String message) {
            this.message = message;
            return this;
        }

        public Builder<T> data(T data) {
            this.data = data;
            return this;
        }

        public Builder<T> timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public BaseResponse<T> build() {
            BaseResponse<T> response = new BaseResponse<>(status, code, message, data);
            response.setTimestamp(timestamp);
            return response;
        }
    }
}
