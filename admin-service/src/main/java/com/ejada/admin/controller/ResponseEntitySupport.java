package com.ejada.admin.controller;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseEntitySupport {

    private ResponseEntitySupport() {
    }

    public static <T> ResponseEntity<BaseResponse<T>> build(
        BaseResponse<T> response,
        HttpStatus successStatus
    ) {
        HttpStatus status = determineStatus(response, successStatus);
        return ResponseEntity.status(status).body(response);
    }

    private static HttpStatus determineStatus(BaseResponse<?> response, HttpStatus successStatus) {
        if (response == null) {
            return successStatus;
        }

        if (response.isSuccess()) {
            return successStatus;
        }

        if (response.isWarning()) {
            return HttpStatus.OK;
        }

        if (response.isError()) {
            return mapErrorCode(response.getCode());
        }

        return successStatus;
    }

    private static HttpStatus mapErrorCode(String code) {
        if (code == null || code.isBlank()) {
            return HttpStatus.BAD_REQUEST;
        }

        return switch (code) {
            case ErrorCodes.NOT_FOUND, ErrorCodes.DATA_NOT_FOUND, ErrorCodes.TENANT_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCodes.AUTH_UNAUTHORIZED, ErrorCodes.AUTH_INVALID_TOKEN, ErrorCodes.AUTH_MISSING_CREDENTIALS,
                ErrorCodes.AUTH_EXPIRED_TOKEN -> HttpStatus.UNAUTHORIZED;
            case ErrorCodes.AUTH_FORBIDDEN, ErrorCodes.TENANT_ACCESS_DENIED -> HttpStatus.FORBIDDEN;
            case ErrorCodes.TENANT_DISABLED -> HttpStatus.LOCKED;
            case ErrorCodes.DATA_DUPLICATE -> HttpStatus.CONFLICT;
            case ErrorCodes.DATA_INTEGRITY, ErrorCodes.BUSINESS_RULE_VIOLATION -> HttpStatus.CONFLICT;
            case ErrorCodes.API_UNPROCESSABLE_ENTITY, ErrorCodes.VALIDATION_ERROR -> HttpStatus.UNPROCESSABLE_ENTITY;
            case ErrorCodes.API_UNSUPPORTED_MEDIA -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case ErrorCodes.API_RATE_LIMIT_EXCEEDED -> HttpStatus.TOO_MANY_REQUESTS;
            case ErrorCodes.SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case ErrorCodes.INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
            case ErrorCodes.DEPENDENCY_FAILURE -> HttpStatus.FAILED_DEPENDENCY;
            case ErrorCodes.TIMEOUT, ErrorCodes.PAYMENT_TIMEOUT -> HttpStatus.GATEWAY_TIMEOUT;
            case ErrorCodes.API_BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case ErrorCodes.PAYMENT_DECLINED, ErrorCodes.PAYMENT_FAILED -> HttpStatus.PAYMENT_REQUIRED;
            default -> HttpStatus.BAD_REQUEST;
        };
    }
}
