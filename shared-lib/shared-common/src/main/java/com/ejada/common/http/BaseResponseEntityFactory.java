package com.ejada.common.http;

import com.ejada.common.dto.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Utility methods for converting {@link BaseResponse} payloads to {@link ResponseEntity} instances
 * with HTTP statuses derived from the response metadata.
 */
public final class BaseResponseEntityFactory {

    private BaseResponseEntityFactory() {
    }

    public static <T> ResponseEntity<BaseResponse<T>> build(final BaseResponse<T> response) {
        return build(response, null);
    }

    public static <T> ResponseEntity<BaseResponse<T>> build(
            final BaseResponse<T> response,
            final HttpStatus successStatusOverride) {

        HttpStatus status = ApiStatusMapper.toHttpStatus(response);
        if (response == null) {
            return ResponseEntity.status(status).build();
        }

        if (successStatusOverride != null && response.isSuccess() && status.is2xxSuccessful()) {
            status = successStatusOverride;
        }

        return ResponseEntity.status(status).body(response);
    }
}
