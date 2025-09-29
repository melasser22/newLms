package com.ejada.subscription.dto;

import jakarta.validation.constraints.NotNull;

/** Generic wrapper matching marketplace shape (code/desc/details + payload). */
public record ServiceResult<T>(
    @NotNull String statusCode,       // e.g., I000000 | EINT000
    @NotNull String statusDescription, // e.g., Successful Operation
    String statusDetails,              // optional details (JSON string if needed)
    T payload                          // response body (or null)
) {

    /**
     * Convenience accessor indicating whether the result represents a successful operation.
     *
     * <p>Marketplace success codes follow the pattern {@code Ixxxxxx}. We therefore treat any
     * status code starting with {@code I} as a success response.</p>
     *
     * @return {@code true} when the {@link #statusCode()} denotes a success, {@code false}
     *     otherwise.
     */
    public boolean success() {
        return statusCode != null && statusCode.startsWith("I");
    }

    /**
     * Convenience accessor that negates {@link #success()} for readability at call sites.
     *
     * @return {@code true} when the result is not successful.
     */
    public boolean failure() {
        return !success();
    }
}
