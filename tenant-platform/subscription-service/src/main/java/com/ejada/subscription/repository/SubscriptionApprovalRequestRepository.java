package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionApprovalRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionApprovalRequestRepository
        extends JpaRepository<SubscriptionApprovalRequest, Long> {

    List<SubscriptionApprovalRequest> findBySubscriptionSubscriptionIdOrderByRequestedAtDesc(Long subscriptionId);

    Optional<SubscriptionApprovalRequest> findFirstBySubscriptionSubscriptionIdAndStatusInOrderByRequestedAtDesc(
            Long subscriptionId, List<String> statuses);
}
