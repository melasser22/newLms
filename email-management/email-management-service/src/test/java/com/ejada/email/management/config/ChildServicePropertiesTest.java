package com.ejada.email.management.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class ChildServicePropertiesTest {

  @Test
  void getTemplateShouldExposeBackingInstance() {
    ChildServiceProperties properties = new ChildServiceProperties();
    URI templateUri = URI.create("https://template.test");

    properties.getTemplate().setBaseUrl(templateUri);

    assertThat(properties.getTemplate().getBaseUrl()).isEqualTo(templateUri);
  }

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
}
