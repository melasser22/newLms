package com.ejada.email.management.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "child-services")
public class ChildServiceProperties {

  private ServiceEndpoint template = new ServiceEndpoint();
  private ServiceEndpoint sending = new ServiceEndpoint();
  private ServiceEndpoint webhook = new ServiceEndpoint();
  private ServiceEndpoint usage = new ServiceEndpoint();

  public ChildServiceProperties() {
    // default constructor
  }

  private ChildServiceProperties(ChildServiceProperties other) {
    this.template = copyOf(other.template);
    this.sending = copyOf(other.sending);
    this.webhook = copyOf(other.webhook);
    this.usage = copyOf(other.usage);
  }

  public ServiceEndpoint getTemplate() {
    return copyOf(template);
  }

  public void setTemplate(ServiceEndpoint template) {
    this.template = copyOf(template);
  }

  public ServiceEndpoint getSending() {
    return copyOf(sending);
  }

  public void setSending(ServiceEndpoint sending) {
    this.sending = copyOf(sending);
  }

  public ServiceEndpoint getWebhook() {
    return copyOf(webhook);
  }

  public void setWebhook(ServiceEndpoint webhook) {
    this.webhook = copyOf(webhook);
  }

  public ServiceEndpoint getUsage() {
    return copyOf(usage);
  }

  public void setUsage(ServiceEndpoint usage) {
    this.usage = copyOf(usage);
  }

  public ChildServiceProperties copy() {
    return new ChildServiceProperties(this);
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

  private static ServiceEndpoint copyOf(ServiceEndpoint source) {
    if (source == null) {
      return new ServiceEndpoint();
    }
    return new ServiceEndpoint(source);
  }
}
