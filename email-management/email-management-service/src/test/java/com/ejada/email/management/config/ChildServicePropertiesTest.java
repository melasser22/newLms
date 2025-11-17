package com.ejada.email.management.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ChildServicePropertiesTest {

  @Test
  void settersShouldUpdateBaseUrl() {
    ChildServiceProperties.ServiceEndpoint endpoint = new ChildServiceProperties.ServiceEndpoint();
    URI baseUrl = URI.create("https://child.test/api");

    endpoint.setBaseUrl(baseUrl);

    assertThat(endpoint.getBaseUrl()).isEqualTo(baseUrl);
  }

  @Test
  void settersShouldCopyEndpoints() {
    ChildServiceProperties properties = new ChildServiceProperties();
    ChildServiceProperties.ServiceEndpoint endpoint = new ChildServiceProperties.ServiceEndpoint();
    URI baseUrl = URI.create("https://child.test/api");
    endpoint.setBaseUrl(baseUrl);

    properties.setTemplate(endpoint);

    assertThat(properties.getTemplate().getBaseUrl()).isEqualTo(baseUrl);
    assertThat(properties.getTemplate()).isNotSameAs(endpoint);
  }

  @Test
  void getterShouldReturnCopyWithConfiguredValue() {
    ChildServiceProperties properties = new ChildServiceProperties();
    ChildServiceProperties.ServiceEndpoint endpoint = new ChildServiceProperties.ServiceEndpoint();
    URI baseUrl = URI.create("https://template.test");
    endpoint.setBaseUrl(baseUrl);

    properties.setTemplate(endpoint);

    ChildServiceProperties.ServiceEndpoint copy = properties.getTemplate();

    assertThat(copy.getBaseUrl()).isEqualTo(baseUrl);
    copy.setBaseUrl(URI.create("https://other.test"));

    assertThat(properties.getTemplate().getBaseUrl()).isEqualTo(baseUrl);
  }
}
