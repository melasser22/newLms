package com.ejada.sec.handler;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.common.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthExceptionHandlerTest {

  private final AuthExceptionHandler handler = new AuthExceptionHandler();

  @Test
  void handleDataAccessReturnsServiceUnavailable() {
    ResponseEntity<ErrorResponse> response =
        handler.handleDataAccess(new DataAccessResourceFailureException("db down"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo("ERR_AUTH_DATA_ACCESS");
  }
}
