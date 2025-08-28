package com.common.starter.core.exception;

import com.common.dto.ErrorResponse;
import com.common.exception.SharedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSharedExceptionReturnsBadRequest() {
        SharedException ex = new SharedException("ERR", "msg", "detail");
        ResponseEntity<ErrorResponse> response = handler.handleSharedException(ex);
        assertEquals(400, response.getStatusCode().value());
        assertEquals("ERR", response.getBody().getCode());
    }

    @Test
    void handleGenericExceptionReturnsInternalServerError() {
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(new RuntimeException("boom"));
        assertEquals(500, response.getStatusCode().value());
        assertEquals("GENERIC_ERROR", response.getBody().getCode());
    }
}
