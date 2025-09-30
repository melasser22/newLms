package com.ejada.common.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Generic marketplace response wrapper shared across services.
 *
 * @param requestId correlating request identifier (optional)
 * @param statusCode marketplace status code (e.g. I000000, EINT000)
 * @param statusDescription human readable description for the status code
 * @param statusDetails collection of optional diagnostic details
 * @param payload response payload
 * @param debugId optional debug identifier used for tracing failures
 * @param <T> type of the wrapped payload
 */
public record ServiceResult<T>(
        String requestId,
        @NotBlank String statusCode,
        @NotBlank String statusDescription,
        List<String> statusDetails,
        T payload,
        String debugId) {

    public ServiceResult {
        statusDetails = statusDetails == null ? List.of() : List.copyOf(statusDetails);
    }

    /**
     * @return {@code true} when the response denotes a successful operation.
     */
    public boolean success() {
        return statusCode != null && statusCode.startsWith("I");
    }

    /**
     * @return {@code true} when the response does not denote a successful operation.
     */
    public boolean failure() {
        return !success();
    }

    public static <T> ServiceResult<T> ok(final T payload) {
        return ok(null, payload);
    }

    public static <T> ServiceResult<T> ok(final String requestId, final T payload) {
        return ok(requestId, payload, "Successful Operation");
    }

    public static <T> ServiceResult<T> ok(
            final String requestId, final T payload, final String description) {
        return new ServiceResult<>(requestId, "I000000", description, List.of(), payload, null);
    }

    public static <T> ServiceResult<T> error(
            final String requestId, final String debugId, final List<String> details) {
        return error(requestId, "EINT000", "Unexpected Error", details, debugId);
    }

    public static <T> ServiceResult<T> error(
            final String requestId,
            final String statusCode,
            final String description,
            final List<String> details) {
        return error(requestId, statusCode, description, details, null);
    }

    public static <T> ServiceResult<T> error(
            final String requestId,
            final String statusCode,
            final String description,
            final List<String> details,
            final String debugId) {
        return new ServiceResult<>(requestId, statusCode, description, details, null, debugId);
    }

    public static <T> ServiceResult<T> withSingleDetail(
            final String requestId,
            final String statusCode,
            final String description,
            final String detail,
            final T payload) {
        List<String> details = (detail == null || detail.isBlank()) ? List.of() : List.of(detail);
        return new ServiceResult<>(requestId, statusCode, description, details, payload, null);
    }
}
