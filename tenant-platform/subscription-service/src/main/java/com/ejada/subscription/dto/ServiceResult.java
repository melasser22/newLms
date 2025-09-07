package com.ejada.subscription.dto;

import jakarta.validation.constraints.NotNull;

/** Generic wrapper matching marketplace shape (code/desc/details + payload). */
public record ServiceResult<T>(
    @NotNull String statusCode,       // e.g., I000000 | EINT000
    @NotNull String statusDescription, // e.g., Successful Operation
    String statusDetails,              // optional details (JSON string if needed)
    T payload                          // response body (or null)
) {}
