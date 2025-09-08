package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.resource.NoResourceFoundException;

/**
 * Handles {@link NoResourceFoundException} when Spring MVC is available.
 */
@RestControllerAdvice
public class NoResourceFoundExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(NoResourceFoundExceptionHandler.class);

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No resource found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("ERR_RESOURCE_NOT_FOUND", ex.getMessage()));
    }
}
