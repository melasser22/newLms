package com.ejada.subscription.repository;

import com.ejada.subscription.model.InboundNotificationAudit;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InboundNotificationAuditRepository extends JpaRepository<InboundNotificationAudit, Long>, JpaSpecificationExecutor<InboundNotificationAudit> {

    Optional<InboundNotificationAudit> findByRqUidAndEndpoint(UUID rqUid, String endpoint);

    boolean existsByRqUidAndEndpoint(UUID rqUid, String endpoint);

    @Modifying
    @Query("update InboundNotificationAudit a set a.processed = true, a.processedAt = current timestamp, a.statusCode = :code, a.statusDesc = :desc, a.statusDtls = :details where a.inboundNotificationAuditId = :id")
    int markProcessed(Long id, String code, String desc, String details);
}
