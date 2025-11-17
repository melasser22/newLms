package com.ejada.push.management.service;

import com.ejada.push.management.config.ChildServiceProperties;
import com.ejada.push.management.dto.UsageSummary;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class UsageGatewayService {

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public UsageGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = properties;
    this.invoker = invoker;
  }

  public UsageSummary getDailySummary(String tenantId) {
    URI baseUrl = requireEndpoint(properties.getUsage().getBaseUrl(), "usage");
    return invoker.get(
        baseUrl,
        "/api/v1/tenants/" + tenantId + "/usage/daily",
        UsageSummary.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
