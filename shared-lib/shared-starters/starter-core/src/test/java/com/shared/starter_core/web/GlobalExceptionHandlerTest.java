package com.shared.starter_core.web;

import com.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleOtherReturnsInternalServerError() {
        ResponseEntity<ErrorResponse> resp = handler.handleOther(new RuntimeException("boom"));
        assertEquals(500, resp.getStatusCode().value());
        assertEquals("GLB-500", resp.getBody().getCode());
        assertEquals("ERROR", resp.getBody().getStatus().name());
    }
}
