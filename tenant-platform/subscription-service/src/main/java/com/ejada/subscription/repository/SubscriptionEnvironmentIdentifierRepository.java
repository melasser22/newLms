package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionEnvironmentIdentifierRepository extends JpaRepository<SubscriptionEnvironmentIdentifier, Long>, JpaSpecificationExecutor<SubscriptionEnvironmentIdentifier> {

    List<SubscriptionEnvironmentIdentifier> findBySubscription_SubscriptionId(Long subscriptionId);

    Optional<SubscriptionEnvironmentIdentifier> findBySubscription_SubscriptionIdAndIdentifierCd(Long subscriptionId, String identifierCd);

    boolean existsBySubscription_SubscriptionIdAndIdentifierCd(Long subscriptionId, String identifierCd);
}
