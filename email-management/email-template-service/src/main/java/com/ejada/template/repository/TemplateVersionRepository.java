package com.ejada.template.repository;

import com.ejada.template.domain.entity.TemplateVersionEntity;
import com.ejada.template.domain.enums.TemplateVersionStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateVersionRepository extends JpaRepository<TemplateVersionEntity, Long> {
  Optional<TemplateVersionEntity> findFirstByTemplateIdOrderByVersionNumberDesc(Long templateId);

  Optional<TemplateVersionEntity> findFirstByTemplateIdAndStatusOrderByVersionNumberDesc(
      Long templateId, TemplateVersionStatus status);
}
