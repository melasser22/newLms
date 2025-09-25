package com.ejada.sec.repository;

import com.ejada.sec.domain.SuperadminAuditLog;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperadminAuditLogRepository extends JpaRepository<SuperadminAuditLog, Long> {

    List<SuperadminAuditLog> findBySuperadminIdOrderByTimestampDesc(Long superadminId);
}
