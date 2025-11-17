package com.ejada.email.template.service.support;

import com.ejada.common.context.ContextManager;
import com.ejada.common.exception.ValidationException;
import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.domain.enums.TemplateVersionStatus;
import com.ejada.email.template.exception.TemplateVersionNotFoundException;
import com.ejada.email.template.repository.TemplateVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TemplateLookupService {

  private final TemplateVersionRepository templateVersionRepository;

  @Cacheable(
      value = "activeTemplateVersions",
      key =
          "T(com.ejada.common.context.ContextManager$Tenant).get() + ':' + #templateId + ':active'")
  public TemplateVersionEntity getActivePublishedVersion(Long templateId) {
    String tenantId = ContextManager.Tenant.get();
    if (tenantId == null) {
      throw new ValidationException("Tenant context is missing", "tenantId is required");
    }
    return templateVersionRepository
        .findFirstByTemplateIdAndTemplate_TenantIdAndStatusOrderByVersionNumberDesc(
            templateId, tenantId, TemplateVersionStatus.PUBLISHED)
        .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, null));
  }
}
