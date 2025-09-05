package com.ejada.setup.web;

import com.ejada.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Module-level exception handler that converts common exceptions into
 * standardized {@link ErrorResponse} payloads. This compliments the
 * global handler provided by starter-core and demonstrates how modules
 * can hook in additional logic.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handle simple {@link IllegalArgumentException}s thrown by controllers
   * or services.
   *
   * @param ex the offending exception
   * @return standardized error response with HTTP 400 status
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    log.error("Invalid request", ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ErrorResponse.of("ERR-400", ex.getMessage()));
  }
}

