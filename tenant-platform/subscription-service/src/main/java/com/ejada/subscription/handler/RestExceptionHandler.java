package com.ejada.subscription.handler;

import com.ejada.subscription.dto.auth.ServiceResult;
import com.ejada.subscription.exception.InvalidCredentialsException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingRequestHeaderException;

@RestControllerAdvice(assignableTypes = com.ejada.subscription.controller.SubscriptionAuthController.class)
public class RestExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ServiceResult<Void> handleValidation(
      final MethodArgumentNotValidException ex, final HttpServletRequest request) {
    String rqUid = request.getHeader("rqUID");
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());
    log.warn("Validation failure rqUID={} details={}", rqUid, details);
    return ServiceResult.failure(rqUid, details);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ServiceResult<Void> handleUnreadable(
      final HttpMessageNotReadableException ex, final HttpServletRequest request) {
    String rqUid = request.getHeader("rqUID");
    log.warn("Malformed payload rqUID={}", rqUid, ex);
    return ServiceResult.failure(rqUid, List.of("Malformed request payload"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ServiceResult<Void> handleTypeMismatch(
      final MethodArgumentTypeMismatchException ex, final HttpServletRequest request) {
    String rqUid = request.getHeader("rqUID");
    log.warn("Type mismatch rqUID={}", rqUid, ex);
    return ServiceResult.failure(rqUid, List.of("Invalid request parameter"));
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ServiceResult<Void> handleMissingHeader(final MissingRequestHeaderException ex) {
    log.warn("Missing required header: {}", ex.getHeaderName());
    return ServiceResult.failure(null, List.of("Missing required header: " + ex.getHeaderName()));
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ServiceResult<Void>> handleInvalidCredentials(
      final InvalidCredentialsException ex, final HttpServletRequest request) {
    String rqUid = request.getHeader("rqUID");
    log.info("Authentication failed for rqUID={}: {}", rqUid, ex.getMessage());
    ServiceResult<Void> result =
        ServiceResult.failure(rqUid, ServiceResult.ERROR_CODE, "Unexpected Error", List.of(ex.getMessage()));
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ServiceResult<Void>> handleAll(
      final Exception ex, final HttpServletRequest request) {
    String rqUid = request.getHeader("rqUID");
    String debugId = UUID.randomUUID().toString();
    log.error("Unhandled exception debugId={} rqUID={}", debugId, rqUid, ex);
    ServiceResult<Void> result =
        ServiceResult.failure(rqUid, debugId, List.of("An unexpected error occurred"));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
  }
}
