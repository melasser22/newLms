package com.shared.starter_core.web;

import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;
import java.util.stream.Collectors;

import com.common.dto.ErrorResponse;
import com.common.exception.BusinessRuleException;
import com.common.exception.NotFoundException;
import com.common.exception.SharedException;
import com.shared.starter_core.context.TenantContextHolder;
import com.shared.starter_core.context.TraceContextHolder;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Helpers -----------------------------------------------------------

    private String currentTraceId() {
        // Prefer correlationId from MDC
        String cid = trimToNull(MDC.get("correlationId"));
        if (cid != null) return cid;

        String tid = trimToNull(MDC.get("traceId"));
        if (tid != null) return tid;

        // Or from context holder
        return trimToNull(TraceContextHolder.getTraceId());
    }

    private String currentTenantId() {
        String t = trimToNull(TenantContextHolder.getTenantId());
        if (t != null) return t;

        return trimToNull(MDC.get("tenantId"));
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String resolveCode(Throwable ex, String fallback) {
        if (ex instanceof BusinessRuleException bre) {
            try {
                var m = BusinessRuleException.class.getMethod("getErrorCode");
                Object code = m.invoke(bre);
                if (code != null) return code.toString();
            } catch (Exception ignore) {}
        }
        if (ex instanceof NotFoundException nfe) {
            try {
                var m = NotFoundException.class.getMethod("getErrorCode");
                Object code = m.invoke(nfe);
                if (code != null) return code.toString();
            } catch (Exception ignore) {}
        }
        if (ex instanceof SharedException le) {
            try {
                var m = SharedException.class.getMethod("getErrorCode");
                Object code = m.invoke(le);
                if (code != null) return code.toString();
            } catch (Exception ignore) {}
        }
        return fallback;
    }

    private ResponseEntity<ErrorResponse> respond(HttpStatus status, String code, String message, List<String> errors) {
        String traceId = currentTraceId();
        String tenantId = currentTenantId();
        ErrorResponse body = ErrorResponse.of(code, message, errors, traceId, tenantId);
        return ResponseEntity.status(status).body(body);
    }

    // ---- Validation --------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.toList());

        return respond(HttpStatus.BAD_REQUEST, "GLB-400", "Invalid request", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid parameter: " + ex.getName();
        return respond(HttpStatus.BAD_REQUEST, "GLB-400", message, List.of(message));
    }

    // ---- Not Found ---------------------------------------------------------

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        String code = resolveCode(ex, "GLB-404");
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage() : "Resource not found";
        return respond(HttpStatus.NOT_FOUND, code, message, List.of(message));
    }

    // ---- Business Rule -----------------------------------------------------

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessRuleException ex) {
        String code = resolveCode(ex, "GLB-400");
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage() : "Business rule violated";
        return respond(HttpStatus.BAD_REQUEST, code, message, List.of(message));
    }

    // ---- Generic Shared Exception ---------------------------------------------

    @ExceptionHandler(SharedException.class)
    public ResponseEntity<ErrorResponse> handleShared(SharedException ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        try {
            var m = SharedException.class.getMethod("getHttpStatus");
            Object s = m.invoke(ex);
            if (s instanceof HttpStatus http) status = http;
        } catch (Exception ignore) {}

        String code = resolveCode(ex, "GLB-500");
        String message = (ex.getMessage() != null && !ex.getMessage().isBlank())
                ? ex.getMessage() : "Unexpected server error";
        return respond(status, code, message, List.of(message));
    }

    // ---- Fallback ----------------------------------------------------------

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "GLB-405", "HTTP method not allowed", List.of("HTTP method not allowed"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "GLB-500", "Unexpected server error", List.of("Unexpected server error"));
    }
}
