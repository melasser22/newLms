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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonAccessDeniedHandler implements AccessDeniedHandler {

  private final ObjectMapper mapper;

  public JsonAccessDeniedHandler(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {

    ErrorResponse body = ErrorResponse.of(
        ErrorCodes.AUTH_FORBIDDEN,                         // e.g., "ERR-403" or "ERR-FORBIDDEN"
        WebUtils.safe(ex.getMessage(), "Forbidden"),
        List.of(),
        request.getRequestURI()
    );
    body.setTenantId(ContextManager.Tenant.get());
    body.setCorrelationId(WebUtils.firstNonBlank(
        MDC.get(HeaderNames.CORRELATION_ID),
        request.getHeader(HeaderNames.CORRELATION_ID),
        request.getHeader(HeaderNames.REQUEST_ID)
    ));

    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType("application/json");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    mapper.writeValue(response.getWriter(), body);
  }

}
