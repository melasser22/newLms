
package com.shared.headers.starter.client;

import com.common.constants.HeaderNames;
import com.shared.headers.starter.config.SharedHeadersProperties;
import com.shared.headers.starter.context.HeaderContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

public class FeignHeaderInterceptor implements RequestInterceptor {
  private final SharedHeadersProperties props;
  public FeignHeaderInterceptor(SharedHeadersProperties props) { this.props = props; }
  @Override public void apply(RequestTemplate template) {
    if (!props.getPropagation().isEnabled()) return;
    for (String name : props.getPropagation().getInclude()) {
      String value = switch (name) {
        case HeaderNames.CORRELATION_ID -> HeaderContext.getCorrelationId();
        case HeaderNames.REQUEST_ID -> HeaderContext.getRequestId();
        case HeaderNames.TENANT_ID -> HeaderContext.getTenantId();
        case HeaderNames.USER_ID -> HeaderContext.getUserId();
        default -> null;
      };
      if (value != null && !template.headers().containsKey(name)) {
        template.header(name, value);
      }
    }
  }
}
