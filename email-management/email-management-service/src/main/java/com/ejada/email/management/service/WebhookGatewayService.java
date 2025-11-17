package com.ejada.email.management.service;

import com.ejada.email.management.config.ChildServiceProperties;
import com.ejada.email.management.dto.SendGridWebhookRequest;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class WebhookGatewayService {

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public WebhookGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = properties;
    this.invoker = invoker;
  }

  public void forwardWebhook(SendGridWebhookRequest request) {
    URI baseUrl = requireEndpoint(properties.getWebhook().getBaseUrl(), "webhook");
    invoker.post(baseUrl, "/api/v1/webhooks/sendgrid", request, Void.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
