package com.ejada.email.sending.client;

import com.ejada.email.sending.client.dto.TemplateDescriptor;

public interface TemplateClient {
  TemplateDescriptor fetchTemplate(String tenantId, String templateKey);
}
