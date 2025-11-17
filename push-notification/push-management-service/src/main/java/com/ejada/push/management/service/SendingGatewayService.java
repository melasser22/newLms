package com.ejada.push.management.service;

import com.ejada.push.management.config.ChildServiceProperties;
import com.ejada.push.management.dto.SendRequest;
import com.ejada.push.management.dto.SendResponse;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class SendingGatewayService {

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public SendingGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = properties;
    this.invoker = invoker;
  }

  public SendResponse send(String tenantId, SendRequest request) {
    URI baseUrl = requireEndpoint(properties.getSending().getBaseUrl(), "sending");
    return invoker.post(
        baseUrl,
        "/api/v1/tenants/" + tenantId + "/send",
        request,
        SendResponse.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
