package com.ejada.gateway.error;

import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.BusinessException;
import com.ejada.common.exception.BusinessRuleException;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.exception.SharedException;
import com.ejada.common.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Reactive exception handler that mirrors the behaviour of
 * {@code GlobalExceptionHandler} from the shared servlet stack.
 */
@Component
@Order(-2)
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
    ServerHttpResponse response = exchange.getResponse();

    if (response.isCommitted()) {
      return Mono.error(ex);
    }

    HttpStatus status = determineStatus(ex);
    String errorCode = determineErrorCode(ex, status);
    String message = determineMessage(ex, status);

    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    try {
      BaseResponse<Void> errorResponse = BaseResponse.error(errorCode, message);
      byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
      DataBuffer buffer = response.bufferFactory().wrap(bytes);
      return response.writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize error response", e);
      return response.setComplete();
    }
  }

  private HttpStatus determineStatus(Throwable ex) {
    if (ex instanceof ResponseStatusException rse) {
      HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
      return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (ex instanceof NotFoundException || ex instanceof java.util.NoSuchElementException) {
      return HttpStatus.NOT_FOUND;
    }
    if (ex instanceof DuplicateResourceException || ex instanceof IllegalStateException) {
      return HttpStatus.CONFLICT;
    }
    if (ex instanceof DataIntegrityViolationException) {
      return HttpStatus.CONFLICT;
    }
    if (ex instanceof ValidationException
        || ex instanceof WebExchangeBindException
        || ex instanceof ConstraintViolationException
        || ex instanceof BusinessRuleException
        || ex instanceof BusinessException
        || ex instanceof IllegalArgumentException) {
      return HttpStatus.BAD_REQUEST;
    }
    if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      return HttpStatus.FORBIDDEN;
    }
    LOGGER.error("Unexpected gateway error", ex);
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  private String determineErrorCode(Throwable ex, HttpStatus status) {
    if (ex instanceof ResponseStatusException) {
      if (status == HttpStatus.NOT_FOUND) {
        return "ERR_RESOURCE_NOT_FOUND";
      }
      return status.is5xxServerError() ? "ERR_INTERNAL" : "ERR_STATUS";
    }
    if (ex instanceof NotFoundException || ex instanceof java.util.NoSuchElementException) {
      return "ERR_RESOURCE_NOT_FOUND";
    }
    if (ex instanceof DuplicateResourceException || ex instanceof DataIntegrityViolationException) {
      return "ERR_DATA_CONFLICT";
    }
    if (ex instanceof ValidationException
        || ex instanceof WebExchangeBindException
        || ex instanceof ConstraintViolationException) {
      return "ERR_VALIDATION";
    }
    if (ex instanceof BusinessRuleException) {
      return "ERR_BUSINESS_RULE";
    }
    if (ex instanceof BusinessException) {
      return "ERR_BUSINESS_LOGIC";
    }
    if (ex instanceof IllegalArgumentException) {
      return "ERR_INVALID_ARGUMENT";
    }
    if (ex instanceof IllegalStateException) {
      return "ERR_ILLEGAL_STATE";
    }
    if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      return "ERR_ACCESS_DENIED";
    }
    return "ERR_INTERNAL";
  }

  private String determineMessage(Throwable ex, HttpStatus status) {
    if (ex instanceof SharedException sharedException) {
      String details = sharedException.getDetails();
      if (StringUtils.hasText(details)) {
        return details;
      }
    }
    if (ex instanceof WebExchangeBindException bindException) {
      return extractBindingMessage(bindException);
    }
    if (ex instanceof ConstraintViolationException constraintViolationException) {
      return extractConstraintViolationMessage(constraintViolationException);
    }
    if (ex instanceof ValidationException validationException) {
      String details = validationException.getDetails();
      if (StringUtils.hasText(details)) {
        return details;
      }
    }
    if (ex instanceof ResponseStatusException rse) {
      String reason = rse.getReason();
      return StringUtils.hasText(reason) ? reason : status.getReasonPhrase();
    }
    if (ex instanceof DataIntegrityViolationException) {
      return "Data conflict occurred";
    }
    if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      return "Access denied";
    }
    if (status.is5xxServerError()) {
      return "An unexpected error occurred";
    }
    String message = ex.getMessage();
    if (!StringUtils.hasText(message)) {
      return status.getReasonPhrase();
    }
    return message;
  }

  private String extractBindingMessage(WebExchangeBindException ex) {
    if (ex.getBindingResult() == null || CollectionUtils.isEmpty(ex.getBindingResult().getAllErrors())) {
      return "Request validation failed";
    }
    return ex.getBindingResult().getAllErrors().stream()
        .map(error -> {
          String defaultMessage = error.getDefaultMessage();
          if (StringUtils.hasText(defaultMessage)) {
            return defaultMessage;
          }
          return error.getCode();
        })
        .filter(StringUtils::hasText)
        .collect(Collectors.joining("; "));
  }

  private String extractConstraintViolationMessage(ConstraintViolationException ex) {
    Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
    if (violations == null || violations.isEmpty()) {
      return "Request validation failed";
    }
    return violations.stream()
        .map(violation -> {
          String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : null;
          String message = violation.getMessage();
          if (StringUtils.hasText(path) && StringUtils.hasText(message)) {
            return path + ": " + message;
          }
          if (StringUtils.hasText(message)) {
            return message;
          }
          return path;
        })
        .filter(StringUtils::hasText)
        .collect(Collectors.joining("; "));
  }
}
