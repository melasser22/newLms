package com.ejada.subscription.repository;

import com.ejada.subscription.model.EntitlementCache;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntitlementCacheRepository extends JpaRepository<EntitlementCache, Long>, JpaSpecificationExecutor<EntitlementCache> {

    List<EntitlementCache> findBySubscription_SubscriptionId(Long subscriptionId);

    Optional<EntitlementCache> findBySubscription_SubscriptionIdAndFeatureKey(Long subscriptionId, String featureKey);

    @Modifying
    @Query("delete from EntitlementCache e where e.subscription.subscriptionId = :subscriptionId")
    int deleteBySubscriptionId(Long subscriptionId);
}
