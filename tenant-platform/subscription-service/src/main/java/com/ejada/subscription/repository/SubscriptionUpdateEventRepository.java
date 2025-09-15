package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionUpdateEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionUpdateEventRepository
        extends JpaRepository<SubscriptionUpdateEvent, Long>,
        JpaSpecificationExecutor<SubscriptionUpdateEvent> {

    Optional<SubscriptionUpdateEvent> findByRqUid(UUID rqUid);

    List<SubscriptionUpdateEvent> findByProcessedFalseOrderByReceivedAtAsc();
}
