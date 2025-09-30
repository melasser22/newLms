package com.ejada.common.http;

import com.ejada.common.dto.ErrorResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Factory for building {@link ResponseEntity} instances with {@link ErrorResponse} payloads
 * that align with the shared {@link com.ejada.common.constants.ErrorCodes} to HTTP status
 * mapping.
 */
public final class RestErrorResponseFactory {

    private RestErrorResponseFactory() {
    }

    public static ResponseEntity<ErrorResponse> build(String code, String message) {
        return build(code, message, List.of(), null);
    }

    public static ResponseEntity<ErrorResponse> build(String code, String message, HttpStatus fallbackStatus) {
        return build(code, message, List.of(), fallbackStatus);
    }

    public static ResponseEntity<ErrorResponse> build(String code, String message, List<String> details) {
        return build(code, message, details, null);
    }

    public static ResponseEntity<ErrorResponse> build(String code,
                                                      String message,
                                                      List<String> details,
                                                      HttpStatus fallbackStatus) {
        ErrorResponse body = ErrorResponse.of(code, message, details);
        HttpStatus status = ApiStatusMapper.fromErrorCode(code, fallbackStatus);
        return ResponseEntity.status(status).body(body);
    }
}
