package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;

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
    void handleAccessDeniedReturnsForbidden() {
        AuthorizationDeniedException ex = new AuthorizationDeniedException("denied");
        ResponseEntity<BaseResponse<?>> resp = handler.handleAccessDenied(ex, null);
        assertEquals(403, resp.getStatusCode().value());
        assertEquals("ERR_ACCESS_DENIED", resp.getBody().getCode());
    }
}
