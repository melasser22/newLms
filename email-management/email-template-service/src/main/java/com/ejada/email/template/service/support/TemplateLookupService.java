package com.ejada.email.template.service.support;

import com.ejada.common.context.ContextManager;
import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.domain.enums.TemplateVersionStatus;
import com.ejada.email.template.exception.TemplateVersionNotFoundException;
import com.ejada.email.template.repository.TemplateVersionRepository;
import java.util.Objects;
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
    String tenantId = Objects.requireNonNull(ContextManager.Tenant.get(), "tenantId is required");
    return templateVersionRepository
        .findFirstByTemplateIdAndTemplate_TenantIdAndStatusOrderByVersionNumberDesc(
            templateId, tenantId, TemplateVersionStatus.PUBLISHED)
        .orElseThrow(() -> new TemplateVersionNotFoundException(templateId, null));
  }
}
