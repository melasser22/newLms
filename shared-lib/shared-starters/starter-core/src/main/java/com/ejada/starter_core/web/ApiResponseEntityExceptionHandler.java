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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reactive exception handler mapping common validation errors into {@link ErrorResponse} payloads.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RestControllerAdvice
public class ApiResponseEntityExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                              ServerWebExchange exchange) {
        List<String> details = new ArrayList<>();
        if (ex.getBindingResult() != null) {
            details.addAll(ex.getBindingResult().getFieldErrors().stream()
                    .map(this::formatFieldError)
                    .collect(Collectors.toList()));
            details.addAll(ex.getBindingResult().getGlobalErrors().stream()
                    .map(ge -> ge.getDefaultMessage() != null ? ge.getDefaultMessage() : ge.toString())
                    .collect(Collectors.toList()));
        }
        ErrorResponse body = ErrorResponse.of(ErrorCodes.VALIDATION_ERROR, "Validation failed", details);
        enrich(body, exchange);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException ex, ServerWebExchange exchange) {
        List<String> details = ex.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.toList());
        details.addAll(ex.getGlobalErrors().stream()
                .map(ge -> ge.getDefaultMessage() != null ? ge.getDefaultMessage() : ge.toString())
                .collect(Collectors.toList()));
        ErrorResponse body = ErrorResponse.of(ErrorCodes.VALIDATION_ERROR, "Validation failed", details);
        enrich(body, exchange);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, ServerWebExchange exchange) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(this::formatConstraintViolation)
                .collect(Collectors.toList());
        ErrorResponse body = ErrorResponse.of(ErrorCodes.VALIDATION_ERROR, "Validation failed", details);
        enrich(body, exchange);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleOtherExceptions(Exception ex, ServerWebExchange exchange) {
        int sc = HttpStatus.INTERNAL_SERVER_ERROR.value();
        String code = ErrorCodes.INTERNAL_ERROR;
        String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        ErrorResponse err = ErrorResponse.of(code, message, List.of());
        enrich(err, exchange);
        return new ResponseEntity<>(err, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String formatFieldError(FieldError fe) {
        String field = fe.getField();
        String msg = fe.getDefaultMessage();
        Object rejected = fe.getRejectedValue();
        return rejected != null ? field + ": " + msg + " (rejected=" + rejected + ")" : field + ": " + msg;
    }

    private String formatConstraintViolation(ConstraintViolation<?> v) {
        String path = (v.getPropertyPath() != null ? v.getPropertyPath().toString() : "<unknown>");
        Object invalid = v.getInvalidValue();
        String msg = v.getMessage();
        return invalid != null ? path + ": " + msg + " (rejected=" + invalid + ")" : path + ": " + msg;
    }

    private void enrich(ErrorResponse err, ServerWebExchange exchange) {
        err.setTenantId(ContextManager.Tenant.get());
        String cid = firstNonBlank(
                MDC.get(HeaderNames.CORRELATION_ID),
                exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID),
                exchange.getRequest().getHeaders().getFirst(HeaderNames.REQUEST_ID)
        );
        if (cid != null) {
            exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, cid);
        }
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}

