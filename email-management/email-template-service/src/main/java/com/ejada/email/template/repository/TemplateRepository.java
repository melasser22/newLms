package com.ejada.template.repository;

import com.ejada.template.domain.entity.TemplateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
  Optional<TemplateEntity> findByNameIgnoreCaseAndLocale(String name, String locale);
}
