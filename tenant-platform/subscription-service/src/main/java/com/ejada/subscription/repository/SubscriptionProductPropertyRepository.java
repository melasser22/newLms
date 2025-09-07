package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionProductProperty;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionProductPropertyRepository extends JpaRepository<SubscriptionProductProperty, Long>, JpaSpecificationExecutor<SubscriptionProductProperty> {

    List<SubscriptionProductProperty> findBySubscription_SubscriptionId(Long subscriptionId);

    Optional<SubscriptionProductProperty> findBySubscription_SubscriptionIdAndPropertyCd(Long subscriptionId, String propertyCd);

    boolean existsBySubscription_SubscriptionIdAndPropertyCd(Long subscriptionId, String propertyCd);
}
