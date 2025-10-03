package com.ejada.subscription.repository;

import com.ejada.subscription.model.SubscriptionActivityLog;
import com.ejada.subscription.model.Subscription;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionActivityLogRepository extends JpaRepository<SubscriptionActivityLog, Long> {

    List<SubscriptionActivityLog> findBySubscriptionOrderByPerformedAtDesc(Subscription subscription);
}
