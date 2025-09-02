package com.shared.starter_core.web;

import com.common.dto.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Handles {@link NoResourceFoundException} when Spring MVC is available.
 */
@Slf4j
@RestControllerAdvice
public class NoResourceFoundExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleNoResourceFound(NoResourceFoundException ex, WebRequest request) {
        log.warn("No resource found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("ERR_RESOURCE_NOT_FOUND", ex.getMessage()));
    }
}
