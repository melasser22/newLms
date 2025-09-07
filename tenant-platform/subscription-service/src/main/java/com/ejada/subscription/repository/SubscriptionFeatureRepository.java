package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionFeature;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionFeatureRepository extends JpaRepository<SubscriptionFeature, Long>, JpaSpecificationExecutor<SubscriptionFeature> {

    List<SubscriptionFeature> findBySubscription_SubscriptionId(Long subscriptionId);

    Optional<SubscriptionFeature> findBySubscription_SubscriptionIdAndFeatureCd(Long subscriptionId, String featureCd);

    boolean existsBySubscription_SubscriptionIdAndFeatureCd(Long subscriptionId, String featureCd);
}
