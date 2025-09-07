package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionAdditionalService;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionAdditionalServiceRepository extends JpaRepository<SubscriptionAdditionalService, Long>, JpaSpecificationExecutor<SubscriptionAdditionalService> {

    List<SubscriptionAdditionalService> findBySubscription_SubscriptionId(Long subscriptionId);

    Optional<SubscriptionAdditionalService> findBySubscription_SubscriptionIdAndProductAdditionalServiceId(Long subscriptionId, Long productAdditionalServiceId);
}
