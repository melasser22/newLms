package com.ejada.management.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "child-services")
public class ChildServiceProperties {

  private final ServiceEndpoint template = new ServiceEndpoint();
  private final ServiceEndpoint sending = new ServiceEndpoint();
  private final ServiceEndpoint webhook = new ServiceEndpoint();
  private final ServiceEndpoint usage = new ServiceEndpoint();

  public ServiceEndpoint getTemplate() {
    return template;
  }

  public ServiceEndpoint getSending() {
    return sending;
  }

  public ServiceEndpoint getWebhook() {
    return webhook;
  }

  public ServiceEndpoint getUsage() {
    return usage;
  }

  public static class ServiceEndpoint {
    private URI baseUrl;

    public URI getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
      this.baseUrl = baseUrl;
    }
  }
}
