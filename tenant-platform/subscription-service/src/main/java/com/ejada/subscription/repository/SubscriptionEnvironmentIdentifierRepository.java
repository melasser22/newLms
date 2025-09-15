package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionEnvironmentIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionEnvironmentIdentifierRepository
        extends JpaRepository<SubscriptionEnvironmentIdentifier, Long>,
        JpaSpecificationExecutor<SubscriptionEnvironmentIdentifier> {

    List<SubscriptionEnvironmentIdentifier> findBySubscriptionSubscriptionId(Long subscriptionId);

    Optional<SubscriptionEnvironmentIdentifier> findBySubscriptionSubscriptionIdAndIdentifierCd(
            Long subscriptionId,
            String identifierCd
    );

    boolean existsBySubscriptionSubscriptionIdAndIdentifierCd(Long subscriptionId, String identifierCd);
}
