package com.ejada.sec.repository;

import com.ejada.sec.domain.SuperadminPasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SuperadminPasswordHistoryRepository extends JpaRepository<SuperadminPasswordHistory, Long> {

    List<SuperadminPasswordHistory> findTop5BySuperadminIdOrderByCreatedAtDesc(Long superadminId);
}
