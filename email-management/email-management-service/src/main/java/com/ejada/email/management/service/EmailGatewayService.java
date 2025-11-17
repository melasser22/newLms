package com.ejada.email.management.service;

import com.ejada.email.management.config.ChildServiceProperties;
import com.ejada.email.management.dto.EmailSendRequest;
import com.ejada.email.management.dto.EmailSendResponse;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EmailGatewayService {

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public EmailGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = requireProperties(properties);
    this.invoker = invoker;
  }

  public EmailSendResponse sendEmail(String tenantId, EmailSendRequest request) {
    URI baseUrl = requireEndpoint(properties.getSending().getBaseUrl(), "sending");
    return invoker.post(
        baseUrl, "/api/v1/tenants/" + tenantId + "/emails/send", request, EmailSendResponse.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }

  private ChildServiceProperties requireProperties(ChildServiceProperties properties) {
    Assert.notNull(properties, "Child service properties must not be null");
    return properties.copy();
  }
}
