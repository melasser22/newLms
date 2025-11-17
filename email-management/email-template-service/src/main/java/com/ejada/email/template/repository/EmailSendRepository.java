package com.ejada.template.repository;

import com.ejada.template.domain.entity.EmailSendEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailSendRepository extends JpaRepository<EmailSendEntity, Long> {
  Optional<EmailSendEntity> findByIdempotencyKey(String idempotencyKey);
}
