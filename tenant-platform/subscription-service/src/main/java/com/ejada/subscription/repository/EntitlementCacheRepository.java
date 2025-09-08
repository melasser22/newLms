package com.ejada.subscription.repository;

import com.ejada.subscription.model.EntitlementCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EntitlementCacheRepository extends JpaRepository<EntitlementCache, Long>, JpaSpecificationExecutor<EntitlementCache> {

    List<EntitlementCache> findBySubscriptionSubscriptionId(Long subscriptionId);

    Optional<EntitlementCache> findBySubscriptionSubscriptionIdAndFeatureKey(Long subscriptionId, String featureKey);

    @Modifying
    @Query("delete from EntitlementCache e where e.subscription.subscriptionId = :subscriptionId")
    int deleteBySubscriptionId(Long subscriptionId);
}
