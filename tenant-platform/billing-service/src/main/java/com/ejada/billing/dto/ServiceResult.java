package com.ejada.billing.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Envelope: ServiceResult«TrackProductConsumptionRs» */
public record ServiceResult<T>(
        @NotBlank String rqUID,
        @NotBlank String statusCode,     // I000000 | EINT000
        @NotBlank String statusDesc,     // Successful Operation | Unexpected Error
        T returnedObject,
        String debugId,
        List<String> statusDtls,
        Boolean success
) {
    public ServiceResult {
        statusDtls = statusDtls == null ? List.of() : List.copyOf(statusDtls);
    }

    @Override
    public List<String> statusDtls() {
        return List.copyOf(statusDtls);
    }

    public static <T> ServiceResult<T> ok(final String rqUID, final T body) {
        return new ServiceResult<>(rqUID, "I000000", "Successful Operation", body, null, null, Boolean.TRUE);
    }
    public static <T> ServiceResult<T> error(final String rqUID, final String debugId, final List<String> details) {
        return new ServiceResult<>(rqUID, "EINT000", "Unexpected Error", null, debugId, details, Boolean.FALSE);
    }
}
