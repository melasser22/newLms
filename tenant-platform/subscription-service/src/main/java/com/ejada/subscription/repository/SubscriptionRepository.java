package com.ejada.subscription.repository;

import com.ejada.subscription.model.Subscription;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long>, JpaSpecificationExecutor<Subscription> {

    Optional<Subscription> findByExtSubscriptionIdAndExtCustomerId(Long extSubscriptionId, Long extCustomerId);

    Optional<Subscription> findByExtSubscriptionId(Long extSubscriptionId);

    boolean existsByExtSubscriptionId(Long extSubscriptionId);

    List<Subscription> findByExtCustomerIdAndIsDeletedFalse(Long extCustomerId);

    @Query("select s from Subscription s where s.endDt < :today and s.isDeleted = false")
    List<Subscription> findExpired(LocalDate today);
}
