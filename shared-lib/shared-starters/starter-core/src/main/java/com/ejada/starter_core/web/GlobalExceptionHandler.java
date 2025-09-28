package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.BusinessException;
import com.ejada.common.exception.BusinessRuleException;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.util.ClassUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Global exception handler producing {@link BaseResponse} payloads.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String ACCESS_DENIED_EXCEPTION =
            "org.springframework.security.access.AccessDeniedException";
    private static final String AUTHORIZATION_DENIED_EXCEPTION =
            "org.springframework.security.authorization.AuthorizationDeniedException";
    private static final String NO_RESOURCE_FOUND_EXCEPTION =
            "org.springframework.web.servlet.resource.NoResourceFoundException";

    private static final boolean SECURITY_EXCEPTION_PRESENT =
            ClassUtils.isPresent(ACCESS_DENIED_EXCEPTION, GlobalExceptionHandler.class.getClassLoader())
                    || ClassUtils.isPresent(
                            AUTHORIZATION_DENIED_EXCEPTION, GlobalExceptionHandler.class.getClassLoader());

    private static final boolean NO_RESOURCE_FOUND_PRESENT =
            ClassUtils.isPresent(NO_RESOURCE_FOUND_EXCEPTION, GlobalExceptionHandler.class.getClassLoader());

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<BaseResponse<?>> handleResourceNotFound(RuntimeException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("ERR_RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<BaseResponse<?>> handleValidation(ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_VALIDATION", ex.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<BaseResponse<?>> handleBusinessRule(BusinessRuleException ex, WebRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_BUSINESS_RULE", ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handleBusiness(BusinessException ex, WebRequest request) {
        log.warn("Business logic violation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_BUSINESS_LOGIC", ex.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class})
    public ResponseEntity<BaseResponse<?>> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Invalid argument: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_INVALID_ARGUMENT", ex.getMessage()));
    }

    @ExceptionHandler({IllegalStateException.class})
    public ResponseEntity<BaseResponse<?>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error("ERR_ILLEGAL_STATE", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation errors: {}", errors);
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_VALIDATION", "Validation failed with " + errors.size() + " errors"));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<?>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_CONSTRAINT_VIOLATION", "Constraint violation: " + ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<BaseResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Type mismatch: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_TYPE_MISMATCH", "Invalid parameter type for: " + ex.getName()));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<?>> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Message not readable: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(BaseResponse.error("ERR_INVALID_REQUEST", "Invalid request body"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<BaseResponse<?>> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error("ERR_DATA_CONFLICT", "Data conflict occurred"));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<BaseResponse<?>> handleDuplicateResource(DuplicateResourceException ex, WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(BaseResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<BaseResponse<?>> handleNoSuchElement(NoSuchElementException ex, WebRequest request) {
        log.warn("Resource missing: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(BaseResponse.error("ERR_RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleGeneric(Exception ex, WebRequest request) {
        if (isSecurityException(ex)) {
            log.warn("Access denied: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(BaseResponse.error("ERR_ACCESS_DENIED", "Access denied"));
        }

        if (isNoResourceFound(ex)) {
            log.warn("No resource found: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(BaseResponse.error("ERR_RESOURCE_NOT_FOUND", defaultMessage(ex, "Resource not found")));
        }

        log.error("Unexpected error occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(BaseResponse.error("ERR_INTERNAL", "An unexpected error occurred"));
    }

    private boolean isSecurityException(Throwable ex) {
        if (!SECURITY_EXCEPTION_PRESENT || ex == null) {
            return false;
        }

        String name = ex.getClass().getName();
        return ACCESS_DENIED_EXCEPTION.equals(name) || AUTHORIZATION_DENIED_EXCEPTION.equals(name);
    }

    private boolean isNoResourceFound(Throwable ex) {
        if (!NO_RESOURCE_FOUND_PRESENT || ex == null) {
            return false;
        }

        Class<?> current = ex.getClass();
        while (current != null) {
            if (NO_RESOURCE_FOUND_EXCEPTION.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private String defaultMessage(Exception ex, String fallback) {
        if (ex == null || ex.getMessage() == null || ex.getMessage().isBlank()) {
            return fallback;
        }
        return ex.getMessage();
    }
}
