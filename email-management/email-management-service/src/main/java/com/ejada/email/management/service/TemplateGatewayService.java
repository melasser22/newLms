package com.ejada.email.management.service;

import com.ejada.email.management.config.ChildServiceProperties;
import com.ejada.email.management.dto.TemplateSummary;
import com.ejada.email.management.dto.TemplateSyncRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
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

  public List<TemplateSummary> fetchTemplates(String tenantId) {
    URI baseUrl = requireEndpoint(properties.getTemplate().getBaseUrl(), "template");
    TemplateSummary[] response =
        invoker.get(
            baseUrl,
            "/internal/api/v1/tenants/" + tenantId + "/templates",
            TemplateSummary[].class);
    return response == null ? List.of() : Arrays.asList(response);
  }

  public void requestSync(String tenantId, TemplateSyncRequest request) {
    URI baseUrl = requireEndpoint(properties.getTemplate().getBaseUrl(), "template");
    invoker.post(
        baseUrl,
        "/internal/api/v1/tenants/" + tenantId + "/templates/sync",
        request,
        Void.class);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
