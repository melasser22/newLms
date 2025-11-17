package com.ejada.email.template.repository;

import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.domain.enums.TemplateVersionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, Long> {
  Optional<TemplateVersionEntity> findFirstByTemplateIdOrderByVersionNumberDesc(Long templateId);

  Optional<TemplateVersionEntity> findFirstByTemplateIdAndTemplate_TenantIdOrderByVersionNumberDesc(
      Long templateId, String tenantId);

  Optional<TemplateVersionEntity> findFirstByTemplateIdAndStatusOrderByVersionNumberDesc(
      Long templateId, TemplateVersionStatus status);

  Optional<TemplateVersionEntity>
      findFirstByTemplateIdAndTemplate_TenantIdAndStatusOrderByVersionNumberDesc(
          Long templateId, String tenantId, TemplateVersionStatus status);

  Optional<TemplateVersionEntity> findByIdAndTemplateIdAndTemplate_TenantId(
      Long id, Long templateId, String tenantId);
}
