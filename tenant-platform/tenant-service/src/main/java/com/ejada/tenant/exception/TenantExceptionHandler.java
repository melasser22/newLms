package com.ejada.tenant.exception;

import com.ejada.common.dto.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ejada.tenant.controller")
public class TenantExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantExceptionHandler.class);

    @ExceptionHandler(TenantConflictException.class)
    public ResponseEntity<BaseResponse<Void>> handleTenantConflict(final TenantConflictException ex) {
        TenantErrorCode code = ex.getErrorCode();
        LOGGER.debug("Tenant conflict detected: {}", ex.getMessage());
        BaseResponse<Void> body = BaseResponse.error(code.getCode(), code.getDefaultMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
}
