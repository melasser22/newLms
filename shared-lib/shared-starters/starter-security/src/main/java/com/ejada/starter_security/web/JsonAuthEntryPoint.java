package com.ejada.starter_security.web;

import com.ejada.common.constants.ErrorCodes;
import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class JsonAuthEntryPoint implements ServerAuthenticationEntryPoint {

  private final ObjectMapper mapper;

  public JsonAuthEntryPoint(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException ex) {
    ErrorResponse body = ErrorResponse.of(
        ErrorCodes.AUTH_UNAUTHORIZED,
        WebUtils.safe(ex.getMessage(), "Unauthorized"),
        List.of()
    );
    body.setTenantId(ContextManager.Tenant.get());
    String cid = WebUtils.firstNonBlank(
        MDC.get(HeaderNames.CORRELATION_ID),
        exchange.getRequest().getHeaders().getFirst(HeaderNames.CORRELATION_ID),
        exchange.getRequest().getHeaders().getFirst(HeaderNames.REQUEST_ID)
    );
    if (cid != null) {
      exchange.getResponse().getHeaders().set(HeaderNames.CORRELATION_ID, cid);
    }

    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    return exchange.getResponse().writeWith(
        Mono.fromSupplier(() -> exchange.getResponse().bufferFactory().wrap(writeValue(body)))
    );
  }

  private byte[] writeValue(Object value) {
    try {
      return mapper.writeValueAsBytes(value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
