package com.ejada.email.management.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "child-services")
public class ChildServiceProperties {

  private final ServiceEndpoint template = new ServiceEndpoint();
  private final ServiceEndpoint sending = new ServiceEndpoint();
  private final ServiceEndpoint webhook = new ServiceEndpoint();
  private final ServiceEndpoint usage = new ServiceEndpoint();

  public ServiceEndpoint getTemplate() {
    return new ServiceEndpoint(template);
  }

  public ServiceEndpoint getSending() {
    return new ServiceEndpoint(sending);
  }

  public ServiceEndpoint getWebhook() {
    return new ServiceEndpoint(webhook);
  }

  public ServiceEndpoint getUsage() {
    return new ServiceEndpoint(usage);
  }

  public static class ServiceEndpoint {
    private URI baseUrl;

    public ServiceEndpoint() {
      // default constructor
    }

    private ServiceEndpoint(ServiceEndpoint other) {
      this.baseUrl = other.baseUrl;
    }

    public URI getBaseUrl() {
      return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
      this.baseUrl = baseUrl;
    }
  }
}
