package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionFeature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionFeatureRepository
        extends JpaRepository<SubscriptionFeature, Long>,
        JpaSpecificationExecutor<SubscriptionFeature> {

    List<SubscriptionFeature> findBySubscriptionSubscriptionId(Long subscriptionId);

    Optional<SubscriptionFeature> findBySubscriptionSubscriptionIdAndFeatureCd(Long subscriptionId, String featureCd);

    boolean existsBySubscriptionSubscriptionIdAndFeatureCd(Long subscriptionId, String featureCd);
}
