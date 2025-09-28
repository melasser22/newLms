package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericReturnsInternalServerError() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleGeneric(new RuntimeException("boom"), null);
        assertEquals(500, resp.getStatusCode().value());
        assertEquals("ERR_INTERNAL", resp.getBody().getCode());
    }

    @Test
    void handleIllegalArgumentReturnsBadRequest() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleIllegalArgument(new IllegalArgumentException("invalid"), null);
        assertEquals(400, resp.getStatusCode().value());
        assertEquals("ERR_INVALID_ARGUMENT", resp.getBody().getCode());
    }

    @Test
    void handleIllegalStateReturnsConflict() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleIllegalState(new IllegalStateException("already done"), null);
        assertEquals(409, resp.getStatusCode().value());
        assertEquals("ERR_ILLEGAL_STATE", resp.getBody().getCode());
    }

    @Test
    void handleNoSuchElementReturnsNotFound() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleNoSuchElement(new java.util.NoSuchElementException("missing"), null);
        assertEquals(404, resp.getStatusCode().value());
        assertEquals("ERR_RESOURCE_NOT_FOUND", resp.getBody().getCode());
    }

    @Test
    void handleValidationErrorsIncludesFieldMessages() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "firstLoginRequest");
        bindingResult.addError(new FieldError("firstLoginRequest", "currentPassword", "Current password is required"));
        MethodParameter parameter = new MethodParameter(
                this.getClass().getDeclaredMethod("stubMethod", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<BaseResponse<java.util.Map<String, String>>> resp = handler.handleValidationErrors(ex, null);

        assertEquals(400, resp.getStatusCode().value());
        assertEquals("ERR_VALIDATION", resp.getBody().getCode());
        assertEquals("Validation failed", resp.getBody().getMessage());
        assertNotNull(resp.getBody().getData());
        assertEquals("Current password is required", resp.getBody().getData().get("currentPassword"));
    }

    @SuppressWarnings("unused")
    private void stubMethod(String value) {
        // Used to build MethodParameter for tests
    }
}
