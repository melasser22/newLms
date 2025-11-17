package com.ejada.push.management.service;

import com.ejada.push.management.config.ChildServiceProperties;
import com.ejada.push.management.dto.TemplateResponse;
import com.ejada.push.management.dto.TemplateUpsertRequest;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class TemplateGatewayService {

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public TemplateGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = properties;
    this.invoker = invoker;
  }

  public TemplateResponse upsertTemplate(String tenantId, TemplateUpsertRequest request) {
    URI baseUrl = requireEndpoint(properties.getTemplate().getBaseUrl(), "template");
    return invoker.post(
        baseUrl,
        "/api/v1/tenants/" + tenantId + "/templates",
        request,
        TemplateResponse.class);
  }

  public TemplateResponse getActive(String tenantId, String templateKey) {
    URI baseUrl = requireEndpoint(properties.getTemplate().getBaseUrl(), "template");
    return invoker.get(
        baseUrl,
        "/api/v1/tenants/" + tenantId + "/templates/" + templateKey + "/active",
        TemplateResponse.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
