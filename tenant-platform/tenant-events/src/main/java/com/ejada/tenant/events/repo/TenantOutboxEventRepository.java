package com.ejada.tenant.events.repo;

import com.ejada.tenant.events.entity.TenantOutboxEvent;
import com.ejada.tenant.events.entity.TenantOutboxEvent.Status;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

/** Repository for pending tenant events. */
public interface TenantOutboxEventRepository extends JpaRepository<TenantOutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<TenantOutboxEvent> findByStatusAndAvailableAtLessThanEqualOrderById(
            Status status, Instant availableAt, Pageable pageable);
}
