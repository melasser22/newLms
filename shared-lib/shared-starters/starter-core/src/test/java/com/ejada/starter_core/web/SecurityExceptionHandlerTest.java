package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler = new SecurityExceptionHandler();

    @Test
    void handleAccessDeniedReturnsForbidden() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleAccessDenied(new AccessDeniedException("denied"));
        assertEquals(403, resp.getStatusCode().value());
        assertEquals("ERR_ACCESS_DENIED", resp.getBody().getCode());
    }

    @Test
    void handleAuthorizationDeniedReturnsForbidden() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleAccessDenied(new AuthorizationDeniedException("denied"));
        assertEquals(403, resp.getStatusCode().value());
        assertEquals("ERR_ACCESS_DENIED", resp.getBody().getCode());
    }
}
