package com.ejada.template.repository;

import com.ejada.template.domain.entity.SendGridSettingEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SendGridSettingRepository extends JpaRepository<SendGridSettingEntity, Long> {
  Optional<SendGridSettingEntity> findTopByOrderByIdDesc();
}
