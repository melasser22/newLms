package com.ejada.catalog.exception;

import com.ejada.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ejada.catalog.controller")
public class CatalogExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogExceptionHandler.class);

    @ExceptionHandler(CatalogConflictException.class)
    public ResponseEntity<BaseResponse<Void>> handleConflict(final CatalogConflictException ex) {
        CatalogErrorCode code = ex.getErrorCode();
        LOGGER.debug("Catalog conflict detected: {}", ex.getMessage());
        BaseResponse<Void> body = BaseResponse.error(code.getCode(), code.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
