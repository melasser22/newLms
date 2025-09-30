package com.ejada.sec.handler;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.dto.ErrorResponse;
import com.ejada.common.exception.ValidationException;
import com.ejada.common.http.RestErrorResponseFactory;
import com.ejada.sec.exception.PasswordHistoryUnavailableException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handles authentication-related exceptions and converts them to {@link ErrorResponse} payloads.
 */
@RestControllerAdvice(basePackages = "com.ejada.sec")
public class AuthExceptionHandler {

    @ExceptionHandler({NoSuchElementException.class, IllegalStateException.class})
    public ResponseEntity<ErrorResponse> handleAuthExceptions(RuntimeException ex) {
        String message = safe(ex.getMessage(), "Invalid credentials");
        return RestErrorResponseFactory.build(
                ErrorCodes.AUTH_INVALID_CREDENTIALS,
                message,
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingCredentials(AuthenticationCredentialsNotFoundException ex) {
        String message = safe(ex.getMessage(), "Authentication credentials are required");
        return RestErrorResponseFactory.build(
                ErrorCodes.AUTH_MISSING_CREDENTIALS,
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        String message = safe(ex.getMessage(), "Invalid authentication request");
        return RestErrorResponseFactory.build(
                ErrorCodes.API_BAD_REQUEST,
                message,
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        List<String> details =
            (ex.getDetails() == null || ex.getDetails().isBlank())
                ? List.of()
                : List.of(ex.getDetails());

        return RestErrorResponseFactory.build(
                ex.getErrorCode(),
                safe(ex.getMessage(), "Validation failed"),
                details,
                HttpStatus.BAD_REQUEST
        );
    }
    @ExceptionHandler(PasswordHistoryUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePasswordHistoryUnavailable(PasswordHistoryUnavailableException ex) {
        return RestErrorResponseFactory.build(
                ErrorCodes.AUTH_HISTORY_UNAVAILABLE,
                safe(ex.getMessage(), "Password history service unavailable"),
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex) {
        return RestErrorResponseFactory.build(
                ErrorCodes.AUTH_DATA_ACCESS,
                "Authentication data is temporarily unavailable. Please try again later.",
                HttpStatus.SERVICE_UNAVAILABLE
        );
    }

    private static String safe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
