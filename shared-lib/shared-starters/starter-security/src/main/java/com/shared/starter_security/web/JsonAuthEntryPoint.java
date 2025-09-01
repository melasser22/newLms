package com.shared.starter_security.web;

import com.common.constants.ErrorCodes;
import com.common.constants.HeaderNames;
import com.common.context.ContextManager;
import com.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonAuthEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper mapper;

  public JsonAuthEntryPoint(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException {

    ErrorResponse body = ErrorResponse.of(
        ErrorCodes.AUTH_UNAUTHORIZED,                      // e.g., "ERR-401" or "ERR-UNAUTHORIZED"
        WebUtils.safe(authException.getMessage(), "Unauthorized"),
        List.of(),
        request.getRequestURI()
    );
    // enrich
    body.setTenantId(ContextManager.Tenant.get());
    body.setCorrelationId(WebUtils.firstNonBlank(
        MDC.get(HeaderNames.CORRELATION_ID),
        request.getHeader(HeaderNames.CORRELATION_ID),
        request.getHeader(HeaderNames.REQUEST_ID)
    ));

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    mapper.writeValue(response.getWriter(), body);
  }

}
