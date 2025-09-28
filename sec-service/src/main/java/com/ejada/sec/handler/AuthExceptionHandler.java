package com.ejada.sec.handler;

import com.ejada.common.dto.ErrorResponse;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles authentication-related exceptions and converts them to {@link ErrorResponse} payloads.
 */
@RestControllerAdvice(basePackages = "com.ejada.sec")
public class AuthExceptionHandler {

    @ExceptionHandler({NoSuchElementException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleAuthExceptions(RuntimeException ex) {
        ErrorResponse body = ErrorResponse.of("ERR_AUTH", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        ErrorResponse body = ErrorResponse.of("ERR_AUTH", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}
