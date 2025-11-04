package com.ejada.admin.repository;

import com.ejada.admin.domain.SuperadminPasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperadminPasswordHistoryRepository extends JpaRepository<SuperadminPasswordHistory, Long> {

    List<SuperadminPasswordHistory> findTop5BySuperadminIdOrderByCreatedAtDesc(Long superadminId);
}
