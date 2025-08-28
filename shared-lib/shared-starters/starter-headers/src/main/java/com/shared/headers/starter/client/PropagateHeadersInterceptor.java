package com.shared.headers.starter.client;

import com.common.constants.HeaderNames;
import com.shared.headers.starter.config.SharedHeadersProperties;
import com.common.context.ContextManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;

public class PropagateHeadersInterceptor implements ClientHttpRequestInterceptor {

  private final SharedHeadersProperties props;

  public PropagateHeadersInterceptor(SharedHeadersProperties props) {
    this.props = props;
  }

  @Override
  public @NonNull ClientHttpResponse intercept(
      @NonNull HttpRequest request,
      @NonNull byte[] body,
      @NonNull ClientHttpRequestExecution execution) throws IOException {

    if (props.getPropagation().isEnabled()) {
      HttpHeaders headers = request.getHeaders();
      RequestAttributes ra = RequestContextHolder.getRequestAttributes();
      HttpServletRequest cur = (ra instanceof ServletRequestAttributes) ? ((ServletRequestAttributes) ra).getRequest() : null;

      for (String name : props.getPropagation().getInclude()) {
        String value = switch (name) {
        case HeaderNames.CORRELATION_ID -> ContextManager.getCorrelationId();
        case HeaderNames.REQUEST_ID -> ContextManager.getRequestId();
        case HeaderNames.TENANT_ID -> ContextManager.Tenant.get();
        case HeaderNames.USER_ID -> ContextManager.getUserId();
          default                 -> null;
        };
        if (value == null && cur != null) value = cur.getHeader(name);
        if (value != null && !headers.containsKey(name)) headers.add(name, value);
      }
    }
    return execution.execute(request, body);
  }
}
