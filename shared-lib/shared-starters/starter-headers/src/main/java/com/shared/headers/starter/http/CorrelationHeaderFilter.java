
package com.shared.headers.starter.http;

import com.shared.headers.starter.config.SharedHeadersProperties;
import com.common.context.ContextManager;
import com.shared.headers.starter.util.HeaderUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import com.common.constants.HeaderNames;

import java.io.IOException;
import java.util.HashMap;

public class CorrelationHeaderFilter implements Filter {

  private final SharedHeadersProperties props;

  public CorrelationHeaderFilter(SharedHeadersProperties props) {
    this.props = props;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    String corrName = props.getCorrelation().getHeader();
    String reqName = props.getRequest().getHeader();
    String tenName = props.getTenant().getHeader();
    String userName = props.getUser().getHeader();

    // Prefer any correlation id already present in the logging context
    String correlationId = MDC.get(HeaderNames.CORRELATION_ID);
    String requestId = HeaderUtils.firstNonEmpty(req.getHeader(reqName));
    String tenantId = HeaderUtils.firstNonEmpty(req.getHeader(tenName));
    String userId = HeaderUtils.firstNonEmpty(req.getHeader(userName));

    if (correlationId == null) {
      correlationId = HeaderUtils.firstNonEmpty(req.getHeader(corrName));
    }

    if (correlationId == null && props.getCorrelation().isAutoGenerate()) {
      correlationId = HeaderUtils.uuid();
    }
    if (requestId == null && props.getRequest().isAutoGenerate()) {
      requestId = HeaderUtils.uuid();
    }
    if (tenantId == null && props.getTenant().isAutoGenerate()) {
      tenantId = HeaderUtils.uuid();
    }
    if (userId == null && props.getUser().isAutoGenerate()) {
      userId = HeaderUtils.uuid();
    }

    if (correlationId == null && props.getCorrelation().isMandatory()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, corrName + " header is required");
      return;
    }
    if (requestId == null && props.getRequest().isMandatory()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, reqName + " header is required");
      return;
    }
    if (tenantId == null && props.getTenant().isMandatory()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, tenName + " header is required");
      return;
    }
    if (userId == null && props.getUser().isMandatory()) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, userName + " header is required");
      return;
    }

    // Set in MDC
    if (props.getMdc().isEnabled()) {
      var kv = new HashMap<String,String>();
      kv.put(HeaderNames.CORRELATION_ID, correlationId);
      kv.put(HeaderNames.REQUEST_ID, requestId);
      if (tenantId != null) kv.put(HeaderNames.TENANT_ID, tenantId);
      if (userId != null) kv.put(HeaderNames.USER_ID, userId);
      HeaderUtils.putMdc(kv);
    }

    // Set in ThreadLocal Context
    ContextManager.setCorrelationId(correlationId);
    ContextManager.setRequestId(requestId);
    ContextManager.Tenant.set(tenantId);
    ContextManager.setUserId(userId);

    // Echo back headers on response
    if (correlationId != null) res.setHeader(corrName, correlationId);
    if (requestId != null) res.setHeader(reqName, requestId);
    if (tenantId != null) res.setHeader(tenName, tenantId);
    if (userId != null) res.setHeader(userName, userId);

    try {
      chain.doFilter(request, response);
    } finally {
      // Do not clear correlation id here so downstream filters (e.g. audit) can access it
      ContextManager.clearRequestId();
      ContextManager.Tenant.clear();
      ContextManager.clearUserId();
      if (props.getMdc().isEnabled()) {
        MDC.remove("requestId");
        MDC.remove("tenantId");
        MDC.remove("userId");
      }
    }
  }
}
