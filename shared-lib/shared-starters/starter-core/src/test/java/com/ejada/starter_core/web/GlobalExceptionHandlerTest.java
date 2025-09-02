package com.ejada.starter_core.web;

import com.ejada.common.dto.BaseResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleGenericReturnsInternalServerError() {
        ResponseEntity<BaseResponse<?>> resp = handler.handleGeneric(new RuntimeException("boom"), null);
        assertEquals(500, resp.getStatusCode().value());
        assertEquals("ERR_INTERNAL", resp.getBody().getCode());
    }
}
