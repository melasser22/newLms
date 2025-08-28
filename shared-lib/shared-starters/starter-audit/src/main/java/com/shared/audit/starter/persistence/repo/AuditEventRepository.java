package com.shared.audit.starter.persistence.repo;

import com.shared.audit.starter.persistence.entity.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {}
