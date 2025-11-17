package com.ejada.email.usage.repository;

import com.ejada.email.usage.domain.EmailEventEntity;
import com.ejada.email.usage.domain.EmailEventType;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailEventEntityRepository extends JpaRepository<EmailEventEntity, Long> {

  List<EmailEventEntity> findByTenantIdAndOccurredAtBetween(
      String tenantId, Instant from, Instant to);

  @Query(
      "select count(e) from EmailEventEntity e where e.tenantId = :tenantId and e.type = :type and e.occurredAt between :from and :to")
  long countByTenantAndTypeBetween(
      @Param("tenantId") String tenantId,
      @Param("type") EmailEventType type,
      @Param("from") Instant from,
      @Param("to") Instant to);
}
