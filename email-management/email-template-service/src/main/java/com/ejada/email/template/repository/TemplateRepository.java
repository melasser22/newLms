package com.ejada.email.template.repository;

import com.ejada.email.template.domain.entity.TemplateEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateRepository extends JpaRepository<TemplateEntity, Long> {
  Optional<TemplateEntity> findByNameIgnoreCaseAndLocale(String name, String locale);
}
