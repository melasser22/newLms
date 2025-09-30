package com.ejada.gateway.error;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.BusinessException;
import com.ejada.common.exception.BusinessRuleException;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive exception handler that mirrors the behaviour of
 * {@code GlobalExceptionHandler} from the shared servlet stack.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayErrorWebExceptionHandler.class);

  private final ObjectMapper objectMapper;
  public GatewayErrorWebExceptionHandler(
      @Qualifier("jacksonObjectMapper") ObjectMapper jacksonObjectMapper,
      ObjectProvider<ObjectMapper> objectMapperProvider) {
    this.objectMapper = (jacksonObjectMapper != null) ? jacksonObjectMapper
        : objectMapperProvider.getIfAvailable(ObjectMapper::new);
  }

  @Override
  public @NonNull Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
    if (exchange.getResponse().isCommitted()) {
      return Mono.error(ex);
    }

    ErrorResponse errorResponse = mapException(ex);
    exchange.getResponse().setStatusCode(errorResponse.status());
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

    try {
      byte[] bytes = objectMapper.writeValueAsBytes(errorResponse.body());
      return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize error response", e);
      return exchange.getResponse().setComplete();
    }
  }

  private ErrorResponse mapException(Throwable ex) {
    if (ex instanceof ResponseStatusException rse) {
      HttpStatusCode status = rse.getStatusCode();
      String code = status.value() >= 500 ? "ERR_INTERNAL" : "ERR_STATUS";
      return new ErrorResponse(status, BaseResponse.error(code, rse.getReason()));
    }
    if (ex instanceof NotFoundException || ex instanceof java.util.NoSuchElementException) {
      return new ErrorResponse(HttpStatus.NOT_FOUND, BaseResponse.error("ERR_RESOURCE_NOT_FOUND", ex.getMessage()));
    }
    if (ex instanceof DuplicateResourceException) {
      return new ErrorResponse(HttpStatus.CONFLICT, BaseResponse.error("ERR_DATA_CONFLICT", ex.getMessage()));
    }
    if (ex instanceof DataIntegrityViolationException) {
      return new ErrorResponse(HttpStatus.CONFLICT, BaseResponse.error("ERR_DATA_CONFLICT", "Data conflict occurred"));
    }
    if (ex instanceof ValidationException) {
      return new ErrorResponse(HttpStatus.BAD_REQUEST, BaseResponse.error("ERR_VALIDATION", ex.getMessage()));
    }
    if (ex instanceof BusinessRuleException) {
      return new ErrorResponse(HttpStatus.BAD_REQUEST, BaseResponse.error("ERR_BUSINESS_RULE", ex.getMessage()));
    }
    if (ex instanceof BusinessException) {
      return new ErrorResponse(HttpStatus.BAD_REQUEST, BaseResponse.error("ERR_BUSINESS_LOGIC", ex.getMessage()));
    }
    if (ex instanceof IllegalArgumentException) {
      return new ErrorResponse(HttpStatus.BAD_REQUEST, BaseResponse.error("ERR_INVALID_ARGUMENT", ex.getMessage()));
    }
    if (ex instanceof IllegalStateException) {
      return new ErrorResponse(HttpStatus.CONFLICT, BaseResponse.error("ERR_ILLEGAL_STATE", ex.getMessage()));
    }
    if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      return new ErrorResponse(HttpStatus.FORBIDDEN, BaseResponse.error("ERR_ACCESS_DENIED", "Access denied"));
    }
    LOGGER.error("Unexpected gateway error", ex);
    return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, BaseResponse.error("ERR_INTERNAL", "An unexpected error occurred"));
  }

  private record ErrorResponse(HttpStatusCode status, BaseResponse<?> body) {}
}
