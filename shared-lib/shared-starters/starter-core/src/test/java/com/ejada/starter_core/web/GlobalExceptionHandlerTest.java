package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericReturnsInternalServerError() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleGeneric(new RuntimeException("boom"), null);
        assertEquals(500, resp.getStatusCode().value());
        assertEquals("ERR_INTERNAL", resp.getBody().getCode());
    }

    @Test
    void handleGenericHandlesAccessDenied() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleGeneric(new AccessDeniedException("denied"), null);
        assertEquals(403, resp.getStatusCode().value());
        assertEquals("ERR_ACCESS_DENIED", resp.getBody().getCode());
    }

    @Test
    void handleGenericHandlesAuthorizationDenied() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleGeneric(new AuthorizationDeniedException("denied"), null);
        assertEquals(403, resp.getStatusCode().value());
        assertEquals("ERR_ACCESS_DENIED", resp.getBody().getCode());
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
    void handleGenericHandlesNoResourceFound() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/api");
        ResponseEntity<BaseResponse<?>> resp = handler.handleGeneric(exception, null);
        assertEquals(404, resp.getStatusCode().value());
        assertEquals("ERR_RESOURCE_NOT_FOUND", resp.getBody().getCode());
    }


}
