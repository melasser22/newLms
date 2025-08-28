package com.shared.headers.starter.client;

import com.common.constants.HeaderNames;
import com.shared.headers.starter.config.SharedHeadersProperties;
import com.common.context.ContextManager;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

public class WebClientHeaderCustomizer implements WebClientCustomizer {

  private final SharedHeadersProperties props;

  public WebClientHeaderCustomizer(SharedHeadersProperties props) {
    this.props = props;
  }

  @Override
  public void customize(WebClient.Builder builder) {
    if (props.getPropagation().isEnabled()) {
      builder.filter(injectHeadersFilter());
    }
  }

  private ExchangeFilterFunction injectHeadersFilter() {
    return (request, next) -> {
      HttpHeaders extra = new HttpHeaders();
      for (String name : props.getPropagation().getInclude()) {
        String value = switch (name) {
        case HeaderNames.CORRELATION_ID -> ContextManager.Header.getCorrelationId();
        case HeaderNames.REQUEST_ID -> ContextManager.Header.getRequestId();
        case HeaderNames.TENANT_ID -> ContextManager.Header.getTenantId();
        case HeaderNames.USER_ID -> ContextManager.Header.getUserId();
          default                 -> null;
        };
        if (value != null) extra.add(name, value);
      }
      ClientRequest mutated = ClientRequest.from(request)
          .headers(h -> extra.forEach((k, v) -> v.forEach(val -> h.add(k, val))))
          .build();
      return next.exchange(mutated);
    };
  }
}
