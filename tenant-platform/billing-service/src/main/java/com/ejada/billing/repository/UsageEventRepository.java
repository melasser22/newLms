package com.ejada.billing.repository;

import com.ejada.billing.model.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageEventRepository extends JpaRepository<UsageEvent, Long> {

    Optional<UsageEvent> findFirstByRqUidOrderByReceivedAtDesc(UUID rqUid);
}
