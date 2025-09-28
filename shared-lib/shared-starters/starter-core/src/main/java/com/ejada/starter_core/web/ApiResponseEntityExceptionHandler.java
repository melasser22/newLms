package com.ejada.starter_core.web;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Centralized handler that maps Spring MVC / validation exceptions into Shared ErrorResponse.
 * P0 behavior:
 *  - All validation errors -> ERR-VALIDATION, with field messages.
 *  - Enrich every response with correlationId (from MDC/header) and tenantId (TenantContext).
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@ControllerAdvice
public class ApiResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    // ---- Validation: DTO/body validation (@Valid on @RequestBody) -----------------

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        List<String> details = new ArrayList<>();

        // Field errors: "field: message"
        if (ex.getBindingResult() != null) {
            details.addAll(
                ex.getBindingResult()
                  .getFieldErrors()
                  .stream()
                  .map(fe -> formatFieldError(fe))
                  .collect(Collectors.toList())
            );
            // Global errors (object-level)
            details.addAll(
                ex.getBindingResult()
                  .getGlobalErrors()
                  .stream()
                  .map(ge -> ge.getDefaultMessage() != null ? ge.getDefaultMessage() : ge.toString())
                  .collect(Collectors.toList())
            );
        }

        ErrorResponse body = ErrorResponse.of(
                ErrorCodes.VALIDATION_ERROR,              // "ERR-VALIDATION"
                "Validation failed",
                details
        );
        enrich(body, request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ---- Validation: binding errors on query/path params, form params ------------

    @org.springframework.web.bind.annotation.ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException ex, WebRequest request) {

        List<String> details = ex.getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());

        // Global errors too
        details.addAll(
            ex.getGlobalErrors()
              .stream()
              .map(ge -> ge.getDefaultMessage() != null ? ge.getDefaultMessage() : ge.toString())
              .collect(Collectors.toList())
        );

        ErrorResponse body = ErrorResponse.of(
                ErrorCodes.VALIDATION_ERROR,
                "Validation failed",
                details
        );
        enrich(body, request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ---- Common business exceptions ---------------------------------------------

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Object> handleIllegalArgument(RuntimeException ex, WebRequest request) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
            ? ex.getMessage()
            : "Request validation failed";

        List<String> details = List.of();
        if (ex.getMessage() != null && ex.getMessage().contains(":")) {
            String[] parts = ex.getMessage().split(":", 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                details = List.of(parts[1].trim());
                message = parts[0].trim();
            }
        }

        ErrorResponse body = ErrorResponse.of(
            ErrorCodes.API_BAD_REQUEST,
            message,
            details
        );
        enrich(body, request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Object> handleNotFound(NoSuchElementException ex, WebRequest request) {
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
            ? ex.getMessage()
            : "Requested resource was not found";

        ErrorResponse body = ErrorResponse.of(
            ErrorCodes.NOT_FOUND,
            message,
            List.of()
        );
        enrich(body, request);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // ---- Validation: method-level (@Validated) constraint violations --------------

    @org.springframework.web.bind.annotation.ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        List<String> details = ex.getConstraintViolations()
                .stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.toList());

        ErrorResponse body = ErrorResponse.of(
                ErrorCodes.VALIDATION_ERROR,
                "Validation failed",
                details
        );
        enrich(body, request);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // ---- Fallback for other framework exceptions ---------------------------------

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // Map to a generic ERR-<status>, fallback to INTERNAL when unknown
        int sc = (status != null ? status.value() : 500);
        String code = (sc >= 400 && sc < 600) ? ("ERR-" + sc) : ErrorCodes.INTERNAL_ERROR;

        // Safe message
        String message = (ex.getMessage() == null || ex.getMessage().isBlank())
                ? ex.getClass().getSimpleName()
                : ex.getMessage();

        ErrorResponse err = ErrorResponse.of(
                code,
                message,
                List.of()                 // you can add more details here if needed
        );
        enrich(err, request);
        return new ResponseEntity<>(err, status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ---- Helpers -----------------------------------------------------------------

    private String formatFieldError(FieldError fe) {
        String field = fe.getField();
        String msg = fe.getDefaultMessage();
        Object rejected = fe.getRejectedValue();
        if (rejected != null) {
            return field + ": " + msg + " (rejected=" + rejected + ")";
        }
        return field + ": " + msg;
    }

    private String formatConstraintViolation(ConstraintViolation<?> v) {
        String path = (v.getPropertyPath() != null ? v.getPropertyPath().toString() : "<unknown>");
        Object invalid = v.getInvalidValue();
        String msg = v.getMessage();
        if (invalid != null) {
            return path + ": " + msg + " (rejected=" + invalid + ")";
        }
        return path + ": " + msg;
    }

    private void enrich(ErrorResponse err, WebRequest req) {
        // tenant
        err.setTenantId(ContextManager.Tenant.get());

        String cid = firstNonBlank(
                MDC.get(HeaderNames.CORRELATION_ID),
                header(req, HeaderNames.CORRELATION_ID),
                header(req, HeaderNames.REQUEST_ID)
        );
        if (req instanceof ServletWebRequest swr && cid != null) {
            swr.getResponse().setHeader(HeaderNames.CORRELATION_ID, cid);
        }
    }

    private String header(WebRequest req, String name) {
        if (req instanceof ServletWebRequest swr) {
            HttpServletRequest http = swr.getRequest();
            return http.getHeader(name);
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
