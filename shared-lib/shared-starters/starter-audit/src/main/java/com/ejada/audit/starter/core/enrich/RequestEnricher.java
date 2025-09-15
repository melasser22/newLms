package com.ejada.audit.starter.core.enrich;

import com.ejada.audit.starter.api.AuditEvent;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

public class RequestEnricher implements Enricher {
  @Override public void enrich(AuditEvent.Builder b) {
    try {
      var attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        HttpServletRequest req = attrs.getRequest();
        b.resource("path", req.getRequestURI()).resource("method", req.getMethod());
        b.meta("ip", req.getRemoteAddr());
        b.meta("userAgent", req.getHeader("User-Agent"));
      }
    } catch (Throwable ignore) { }
  }
}
