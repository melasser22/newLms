package com.ejada.admin.exception;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.BaseResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException exception
    ) {
        Map<String, String> errors = new HashMap<>();
        exception.getBindingResult().getFieldErrors()
            .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

        log.debug("Validation failed: {}", errors);
        BaseResponse<Map<String, String>> response = BaseResponse.error(
            ErrorCodes.VALIDATION_ERROR,
            "Request validation failed",
            errors
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleConstraintViolation(
        ConstraintViolationException exception
    ) {
        Map<String, String> errors = new HashMap<>();
        exception.getConstraintViolations().forEach(
            violation -> errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        BaseResponse<Map<String, String>> response = BaseResponse.error(
            ErrorCodes.VALIDATION_ERROR,
            "Constraint violation",
            errors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        log.debug("Failed to read request body", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.API_BAD_REQUEST,
            "Malformed JSON request"
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<BaseResponse<Void>> handleNotFound(NoSuchElementException exception) {
        log.debug("Resource not found", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.NOT_FOUND,
            exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<BaseResponse<Void>> handleBadRequest(IllegalArgumentException exception) {
        log.debug("Bad request", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.API_BAD_REQUEST,
            exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<BaseResponse<Void>> handleIllegalState(IllegalStateException exception) {
        log.debug("Illegal state", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.BUSINESS_RULE_VIOLATION,
            exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<BaseResponse<Void>> handleMissingCredentials(
        AuthenticationCredentialsNotFoundException exception
    ) {
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.AUTH_UNAUTHORIZED,
            exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<BaseResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.AUTH_FORBIDDEN,
            exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(PasswordHistoryUnavailableException.class)
    public ResponseEntity<BaseResponse<Void>> handlePasswordHistoryUnavailable(
        PasswordHistoryUnavailableException exception
    ) {
        log.error("Password history verification unavailable", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.SERVICE_UNAVAILABLE,
            exception.getMessage()
        );
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<BaseResponse<Void>> handleDataAccess(DataAccessException exception) {
        log.error("Database operation failed", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.DATA_INTEGRITY,
            "Unable to process the request due to data integrity issues"
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<Void>> handleGenericException(Exception exception) {
        log.error("Unexpected error", exception);
        BaseResponse<Void> response = BaseResponse.error(
            ErrorCodes.INTERNAL_ERROR,
            "An unexpected error occurred"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
