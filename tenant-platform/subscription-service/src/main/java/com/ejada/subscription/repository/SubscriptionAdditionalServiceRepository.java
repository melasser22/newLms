package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionAdditionalService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionAdditionalServiceRepository
        extends JpaRepository<SubscriptionAdditionalService, Long>,
        JpaSpecificationExecutor<SubscriptionAdditionalService> {

    List<SubscriptionAdditionalService> findBySubscriptionSubscriptionId(Long subscriptionId);

    Optional<SubscriptionAdditionalService> findBySubscriptionSubscriptionIdAndProductAdditionalServiceId(
            Long subscriptionId,
            Long productAdditionalServiceId
    );
}
