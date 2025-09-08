package com.ejada.sec.handler;

import com.ejada.common.dto.BaseResponse;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles authentication-related exceptions and converts them to {@link BaseResponse} payloads.
 */
@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler({NoSuchElementException.class, IllegalStateException.class})
    public ResponseEntity<BaseResponse<?>> handleAuthExceptions(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(BaseResponse.error("ERR_AUTH", ex.getMessage()));
    }
}
