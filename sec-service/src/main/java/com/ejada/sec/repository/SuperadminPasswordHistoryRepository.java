package com.ejada.sec.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;

@Repository
public interface SuperadminPasswordHistoryRepository{}

//extends JpaRepository<SuperadminPasswordHistory, Long> {
//
//    List<SuperadminPasswordHistory> findTop5BySuperadminIdOrderByCreatedAtDesc(Long superadminId);
//
//}
