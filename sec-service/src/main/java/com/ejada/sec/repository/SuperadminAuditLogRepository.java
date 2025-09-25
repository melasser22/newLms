package com.ejada.sec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SuperadminAuditLogRepository {}

//extends JpaRepository<SuperadminAuditLog, Long> {
//
//    List<SuperadminAuditLog> findBySuperadminIdOrderByTimestampDesc(Long superadminId);
//
//}
