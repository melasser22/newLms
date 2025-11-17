package com.ejada.sending.client.impl;

import com.ejada.sending.client.TemplateClient;
import com.ejada.sending.client.dto.TemplateDescriptor;
import com.ejada.sending.config.EmailSendingProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Dependencies are injected and not exposed")
public class HttpTemplateClient implements TemplateClient {

  private static final Logger log = LoggerFactory.getLogger(HttpTemplateClient.class);

  private final RestTemplate restTemplate;
  private final EmailSendingProperties properties;

  public HttpTemplateClient(RestTemplate restTemplate, EmailSendingProperties properties) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  @Override
  public TemplateDescriptor fetchTemplate(String tenantId, String templateKey) {
    if (properties.getTemplateServiceBaseUrl() == null || properties.getTemplateServiceBaseUrl().isBlank()) {
      return new TemplateDescriptor(templateKey, null, false);
    }

    String url = properties.getTemplateServiceBaseUrl() + "/api/v1/tenants/" + tenantId + "/templates/" + templateKey;
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.add("X-Tenant-ID", tenantId);
      ResponseEntity<TemplateDescriptor> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), TemplateDescriptor.class);
      if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
        return response.getBody();
      }
      log.warn("Template service returned non-success status {} for template {}", response.getStatusCode(), templateKey);
    } catch (RestClientException ex) {
      log.warn("Failed to fetch template {} for tenant {}", templateKey, tenantId, ex);
    }
    return new TemplateDescriptor(templateKey, null, false);
  }
}
