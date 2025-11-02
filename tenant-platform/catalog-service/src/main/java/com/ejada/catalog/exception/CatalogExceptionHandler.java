package com.ejada.catalog.exception;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.SharedException;
import com.ejada.common.http.ApiStatusMapper;
import jakarta.persistence.EntityNotFoundException;
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

    @ExceptionHandler(SharedException.class)
    public ResponseEntity<BaseResponse<Void>> handleSharedException(final SharedException ex) {
        LOGGER.debug("Catalog shared exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        BaseResponse<Void> body = BaseResponse.<Void>builder()
                .status(ex.getStatus())
                .code(ex.getErrorCode())
                .message(ex.getMessage())
                .build();
        HttpStatus status = ApiStatusMapper.fromErrorCode(ex.getErrorCode(), ApiStatusMapper.toHttpStatus(body));
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleEntityNotFound(final EntityNotFoundException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Resource not found";
        LOGGER.debug("Catalog entity not found: {}", message);
        BaseResponse<Void> body = BaseResponse.error(ErrorCodes.NOT_FOUND, message);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
}
