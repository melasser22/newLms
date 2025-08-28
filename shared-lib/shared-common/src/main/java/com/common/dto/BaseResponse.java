package com.common.dto;

import com.common.enums.StatusEnums.ApiStatus;
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

    // ===== Static builders (nice usability) =====
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .status(ApiStatus.SUCCESS)
                .code("SUCCESS-200")
                .message("Operation successful")
                .data(data)
                .build();
    }

    /**
     * Create a success response with a custom message.
     *
     * @param message human-readable success message
     * @param data    optional payload
     * @return a new BaseResponse with status SUCCESS
     */
    public static <T> BaseResponse<T> success(String message, T data) {
        return BaseResponse.<T>builder()
                .status(ApiStatus.SUCCESS)
                .code("SUCCESS-200")
                .message(message)
                .data(data)
                .build();
    }

    public static <T> BaseResponse<T> error(String code, String message) {
        return BaseResponse.<T>builder()
                .status(ApiStatus.ERROR)
                .code(code)
                .message(message)
                .build();
    }

    public static <T> BaseResponse<T> warning(String code, String message, T data) {
        return BaseResponse.<T>builder()
                .status(ApiStatus.WARNING)
                .code(code)
                .message(message)
                .data(data)
                .build();
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
                .build();
    }
}

