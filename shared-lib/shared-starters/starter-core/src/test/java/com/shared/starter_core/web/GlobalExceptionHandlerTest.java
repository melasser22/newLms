package com.shared.starter_core.web;

import com.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleOtherReturnsInternalServerError() {
        ResponseEntity<ErrorResponse> resp = handler.handleOther(new RuntimeException("boom"));
        assertThat(resp.getStatusCode().value()).isEqualTo(500);
        assertThat(resp.getBody().getCode()).isEqualTo("GLB-500");
        assertThat(resp.getBody().getStatus().name()).isEqualTo("ERROR");
    }
}
