package com.lms.tenant.events.repo;

import com.lms.tenant.events.entity.TenantOutboxEvent;
import com.lms.tenant.events.entity.TenantOutboxEvent.Status;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository for pending tenant events. */
public interface TenantOutboxEventRepository extends JpaRepository<TenantOutboxEvent, Long> {

    List<TenantOutboxEvent> findByStatusAndAvailableAtLessThanEqualOrderById(
            Status status, Instant availableAt, Pageable pageable);
}
