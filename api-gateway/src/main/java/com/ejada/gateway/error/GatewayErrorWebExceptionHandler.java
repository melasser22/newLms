package com.ejada.gateway.error;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.dto.BaseResponse;
import com.ejada.common.exception.BusinessException;
import com.ejada.common.exception.BusinessRuleException;
import com.ejada.common.exception.DuplicateResourceException;
import com.ejada.common.exception.NotFoundException;
import com.ejada.common.exception.SharedException;
import com.ejada.common.exception.ValidationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ejada.gateway.context.GatewayRequestAttributes;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
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
      @Qualifier("jacksonObjectMapper") ObjectProvider<ObjectMapper> jacksonObjectMapperProvider,
      ObjectProvider<ObjectMapper> objectMapperProvider) {
    ObjectMapper jacksonObjectMapper = jacksonObjectMapperProvider.getIfAvailable();
    this.objectMapper = (jacksonObjectMapper != null) ? jacksonObjectMapper
        : objectMapperProvider.getIfAvailable(ObjectMapper::new);
    LOGGER.info("GatewayErrorWebExceptionHandler initialized with ObjectMapper: {}",
        this.objectMapper.getClass().getName());
  }

  @Override
  public @NonNull Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
    ServerHttpResponse response = exchange.getResponse();

    if (response.isCommitted()) {
      return Mono.error(ex);
    }

    HttpStatus status = determineStatus(ex);
    String errorCode = determineErrorCode(ex, status);
    String correlationId = resolveCorrelationId(exchange);
    String tenantId = resolveTenantId(exchange);
    String message = enhanceMessage(determineMessage(ex, status), status, correlationId);
    Map<String, Object> diagnostics = buildDiagnostics(exchange, status, errorCode, message, correlationId, tenantId);

    // Log the exception details for troubleshooting
    boolean notFound = status == HttpStatus.NOT_FOUND;
    if (notFound) {
      LOGGER.debug("Gateway route not found [path={}, correlationId={}]", exchange.getRequest().getPath(), correlationId);
    } else if (status.is5xxServerError()) {
      LOGGER.error("Gateway error [correlationId={}, tenantId={}, path={}, status={}]: {}",
          correlationId, tenantId, exchange.getRequest().getPath().value(), status.value(), message, ex);
    } else {
      LOGGER.debug("Gateway client error [correlationId={}, path={}, status={}]: {}",
          correlationId, exchange.getRequest().getPath().value(), status.value(), message);
    }

    response.setStatusCode(status);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

    try {
      BaseResponse<Map<String, Object>> errorResponse = BaseResponse.error(errorCode, message, diagnostics);
      byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
      DataBuffer buffer = response.bufferFactory().wrap(bytes);
      return response.writeWith(Mono.just(buffer));
    } catch (JsonProcessingException e) {
      LOGGER.error("Failed to serialize error response [correlationId={}]: {}", correlationId, e.getMessage(), e);
      // Fallback to plain text error response
      return writeFallbackErrorResponse(response, status, errorCode, message, correlationId);
    } catch (Exception e) {
      LOGGER.error("Unexpected error in error handler [correlationId={}]: {}", correlationId, e.getMessage(), e);
      return writeFallbackErrorResponse(response, status, errorCode, message, correlationId);
    }
  }

  /**
   * Writes a plain text fallback error response when JSON serialization fails.
   */
  private Mono<Void> writeFallbackErrorResponse(ServerHttpResponse response, HttpStatus status,
      String errorCode, String message, String correlationId) {
    try {
      response.getHeaders().setContentType(MediaType.TEXT_PLAIN);
      String plainTextError = String.format(
          "Error Code: %s%nStatus: %d%nMessage: %s%nCorrelation ID: %s",
          errorCode, status.value(), message, correlationId != null ? correlationId : "unknown");
      DataBuffer buffer = response.bufferFactory().wrap(plainTextError.getBytes());
      return response.writeWith(Mono.just(buffer));
    } catch (Exception fallbackException) {
      LOGGER.error("Failed to write fallback error response", fallbackException);
      return response.setComplete();
    }
  }

  private HttpStatus determineStatus(Throwable ex) {
    if (ex instanceof ResponseStatusException rse) {
      HttpStatus status = HttpStatus.resolve(rse.getStatusCode().value());
      return status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (ex instanceof NotFoundException
        || ex instanceof java.util.NoSuchElementException
        || ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
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

    WebClientRequestException webClientException = resolveWebClientRequestException(ex);
    if (webClientException != null) {
      if (isTimeoutException(webClientException)) {
        return HttpStatus.GATEWAY_TIMEOUT;
      }
      return HttpStatus.SERVICE_UNAVAILABLE;
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
    if (ex instanceof NotFoundException
        || ex instanceof java.util.NoSuchElementException
        || ex instanceof org.springframework.cloud.gateway.support.NotFoundException) {
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
    if (status == HttpStatus.SERVICE_UNAVAILABLE) {
      return "ERR_UPSTREAM_UNAVAILABLE";
    }
    if (status == HttpStatus.GATEWAY_TIMEOUT) {
      return "ERR_UPSTREAM_TIMEOUT";
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
    if (ex instanceof org.springframework.cloud.gateway.support.NotFoundException gatewayNotFound) {
      String reason = gatewayNotFound.getMessage();
      if (StringUtils.hasText(reason)) {
        return reason;
      }
    }
    if (ex instanceof DataIntegrityViolationException) {
      return "Data conflict occurred";
    }
    if (ex instanceof org.springframework.security.access.AccessDeniedException) {
      return "Access denied";
    }
    if (status == HttpStatus.SERVICE_UNAVAILABLE) {
      return "Upstream service is unavailable";
    }
    if (status == HttpStatus.GATEWAY_TIMEOUT) {
      return "Upstream service timed out";
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

  private String enhanceMessage(String message, HttpStatus status, String correlationId) {
    if (status.is5xxServerError() && "An unexpected error occurred".equals(message)) {
      String reference = StringUtils.hasText(correlationId) ? correlationId : "unknown";
      return "An unexpected error occurred. Please contact support with correlation ID " + reference + ".";
    }
    return message;
  }

  private Map<String, Object> buildDiagnostics(ServerWebExchange exchange, HttpStatus status,
      String errorCode, String message, String correlationId, String tenantId) {
    Map<String, Object> diagnostics = new LinkedHashMap<>();
    diagnostics.put("timestamp", Instant.now().toString());
    diagnostics.put("path", exchange.getRequest().getPath().value());
    HttpMethod requestMethod = exchange.getRequest().getMethod();
    diagnostics.put("method", requestMethod != null ? requestMethod.name() : "UNKNOWN");
    diagnostics.put("status", status.value());
    diagnostics.put("errorCode", errorCode);
    diagnostics.put("message", message);
    diagnostics.put("correlationId", StringUtils.hasText(correlationId) ? correlationId : "unknown");
    diagnostics.put("tenantId", StringUtils.hasText(tenantId) ? tenantId : "unknown");
    diagnostics.put("supportUrl", "https://support.example.com/error/" + status.value());
    return diagnostics;
  }

  private String resolveCorrelationId(ServerWebExchange exchange) {
    String correlationId = exchange.getAttribute(GatewayRequestAttributes.CORRELATION_ID);
    if (!StringUtils.hasText(correlationId)) {
      correlationId = trimToNull(exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID));
    }
    if (!StringUtils.hasText(correlationId)) {
      correlationId = trimToNull(ContextManager.getCorrelationId());
    }
    return correlationId;
  }

  private String resolveTenantId(ServerWebExchange exchange) {
    String tenantId = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (!StringUtils.hasText(tenantId)) {
      tenantId = trimToNull(exchange.getRequest().getHeaders().getFirst(HeaderNames.X_TENANT_ID));
    }
    if (!StringUtils.hasText(tenantId)) {
      tenantId = trimToNull(ContextManager.Tenant.get());
    }
    return tenantId;
  }

  private String trimToNull(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
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

  private WebClientRequestException resolveWebClientRequestException(Throwable ex) {
    if (ex instanceof WebClientRequestException requestException) {
      return requestException;
    }
    Set<Throwable> visited = new HashSet<>();
    Throwable current = ex.getCause();
    while (current != null && visited.add(current)) {
      if (current instanceof WebClientRequestException requestException) {
        return requestException;
      }
      current = current.getCause();
    }
    return null;
  }

  private boolean isTimeoutException(WebClientRequestException ex) {
    Set<Throwable> visited = new HashSet<>();
    Throwable current = ex;
    while (current != null && visited.add(current)) {
      if (current instanceof java.util.concurrent.TimeoutException
          || current instanceof ReadTimeoutException
          || current instanceof ConnectTimeoutException) {
        return true;
      }
      if (current instanceof ConnectException || current instanceof UnknownHostException) {
        return false;
      }
      current = current.getCause();
    }
    return false;
  }
}
