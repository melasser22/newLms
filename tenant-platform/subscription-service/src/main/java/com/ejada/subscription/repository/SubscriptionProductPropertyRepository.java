package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionProductProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionProductPropertyRepository
        extends JpaRepository<SubscriptionProductProperty, Long>,
        JpaSpecificationExecutor<SubscriptionProductProperty> {

    List<SubscriptionProductProperty> findBySubscriptionSubscriptionId(Long subscriptionId);

    Optional<SubscriptionProductProperty> findBySubscriptionSubscriptionIdAndPropertyCd(Long subscriptionId, String propertyCd);

    boolean existsBySubscriptionSubscriptionIdAndPropertyCd(Long subscriptionId, String propertyCd);
}
