package com.ejada.common.web;

import com.ejada.common.dto.ServiceResult;
import com.ejada.common.http.ApiStatusMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;

/** Utility methods for converting {@link ServiceResult} objects to {@link ResponseEntity} instances. */
public final class ServiceResultResponses {

    private ServiceResultResponses() {
        // utility
    }

    public static <T> ResponseEntity<ServiceResult<T>> respond(final ServiceResult<T> result) {
        if (result == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
        HttpStatus status = resolveHttpStatus(result);
        return ResponseEntity.status(status).body(result);
    }

    public static HttpStatus resolveHttpStatus(final ServiceResult<?> result) {
        if (result == null || Boolean.TRUE.equals(result.success())) {
            return HttpStatus.OK;
        }
        String statusCode = result.statusCode();
        if (!StringUtils.hasText(statusCode)) {
            return HttpStatus.BAD_REQUEST;
        }
        if (statusCode.startsWith("EINT")) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ApiStatusMapper.fromErrorCode(statusCode, HttpStatus.BAD_REQUEST);
    }
}
