package com.ejada.email.template.repository;

import com.ejada.email.template.domain.entity.TemplateEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
  Optional<TemplateEntity> findByNameIgnoreCaseAndLocale(String name, String locale);

  Optional<TemplateEntity> findByIdAndTenantId(Long id, String tenantId);

  Page<TemplateEntity> findByTenantId(String tenantId, Pageable pageable);
}
