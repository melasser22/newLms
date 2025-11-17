package com.ejada.sending.client;

import com.ejada.sending.client.dto.TemplateDescriptor;

public interface TemplateClient {
  TemplateDescriptor fetchTemplate(String tenantId, String templateKey);
}
