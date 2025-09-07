package com.ejada.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    public static <T> ServiceResult<T> ok(String rqUID, T body) {
        return new ServiceResult<>(rqUID, "I000000", "Successful Operation", body, null, null, Boolean.TRUE);
    }
    public static <T> ServiceResult<T> error(String rqUID, String debugId, List<String> details) {
        return new ServiceResult<>(rqUID, "EINT000", "Unexpected Error", null, debugId, details, Boolean.FALSE);
    }
}
