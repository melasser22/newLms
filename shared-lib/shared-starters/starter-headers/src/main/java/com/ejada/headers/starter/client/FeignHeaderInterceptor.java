
package com.ejada.headers.starter.client;

import com.ejada.common.constants.HeaderNames;
import com.ejada.headers.starter.config.SharedHeadersProperties;
import com.ejada.common.context.ContextManager;
import feign.RequestInterceptor;
import feign.RequestTemplate;

public class FeignHeaderInterceptor implements RequestInterceptor {
  private final SharedHeadersProperties props;
  public FeignHeaderInterceptor(SharedHeadersProperties props) { this.props = props; }
  @Override public void apply(RequestTemplate template) {
    if (!props.getPropagation().isEnabled()) return;
    for (String name : props.getPropagation().getInclude()) {
      String value = switch (name) {
      case HeaderNames.CORRELATION_ID -> ContextManager.getCorrelationId();
      case HeaderNames.REQUEST_ID -> ContextManager.getRequestId();
      case HeaderNames.X_TENANT_ID -> ContextManager.Tenant.get();
      case HeaderNames.USER_ID -> ContextManager.getUserId();
        default -> null;
      };
      if (value != null && !template.headers().containsKey(name)) {
        template.header(name, value);
      }
    }
  }
}
