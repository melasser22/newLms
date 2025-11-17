package com.ejada.email.management.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ChildServicePropertiesTest {

  @Test
  void getTemplateShouldReturnDefensiveCopy() {
    ChildServiceProperties properties = new ChildServiceProperties();
    ChildServiceProperties.ServiceEndpoint internalTemplate =
        (ChildServiceProperties.ServiceEndpoint) ReflectionTestUtils.getField(properties, "template");
    URI templateUri = URI.create("https://template.test");
    ReflectionTestUtils.setField(internalTemplate, "baseUrl", templateUri);

    ChildServiceProperties.ServiceEndpoint retrievedTemplate = properties.getTemplate();

    assertThat(retrievedTemplate).isNotSameAs(internalTemplate);
    assertThat(retrievedTemplate.getBaseUrl()).isEqualTo(templateUri);

    retrievedTemplate.setBaseUrl(URI.create("https://mutated.test"));
    ChildServiceProperties.ServiceEndpoint secondRead = properties.getTemplate();

    assertThat(secondRead.getBaseUrl()).isEqualTo(templateUri);
  }

  @Test
  void settersShouldUpdateBaseUrl() {
    ChildServiceProperties.ServiceEndpoint endpoint = new ChildServiceProperties.ServiceEndpoint();
    URI baseUrl = URI.create("https://child.test/api");

    endpoint.setBaseUrl(baseUrl);

    assertThat(endpoint.getBaseUrl()).isEqualTo(baseUrl);
  }
}
