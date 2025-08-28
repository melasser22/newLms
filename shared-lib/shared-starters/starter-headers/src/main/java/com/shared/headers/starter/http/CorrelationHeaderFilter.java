
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

    String correlationId = HeaderUtils.firstNonEmpty(req.getHeader(corrName));
    String requestId = HeaderUtils.firstNonEmpty(req.getHeader(reqName));
    String tenantId = HeaderUtils.firstNonEmpty(req.getHeader(tenName));
    String userId = HeaderUtils.firstNonEmpty(req.getHeader(userName));

    if (props.isGenerateIfMissing()) {
      if (correlationId == null) correlationId = HeaderUtils.uuid();
      if (requestId == null) requestId = HeaderUtils.uuid();
    }

    // Set in MDC
    if (props.getMdc().isEnabled()) {
      var kv = new HashMap<String,String>();
      kv.put( HeaderNames.CORRELATION_ID, correlationId);
      kv.put(HeaderNames.REQUEST_ID, requestId);
      if (tenantId != null) kv.put(HeaderNames.TENANT_ID, tenantId);
      if (userId != null) kv.put(HeaderNames.USER_ID, userId);
      HeaderUtils.putMdc(kv);
    }

    // Set in ThreadLocal Context
    ContextManager.Header.setCorrelationId(correlationId);
    ContextManager.Header.setRequestId(requestId);
    ContextManager.Header.setTenantId(tenantId);
    ContextManager.Header.setUserId(userId);

    // Echo back headers on response
    if (correlationId != null) res.setHeader(corrName, correlationId);
    if (requestId != null) res.setHeader(reqName, requestId);

    try {
      chain.doFilter(request, response);
    } finally {
      // cleanup
    	ContextManager.Header.clear();
      if (props.getMdc().isEnabled()) {
        MDC.remove("correlationId");
        MDC.remove("requestId");
        MDC.remove("tenantId");
        MDC.remove("userId");
      }
    }
  }
}
