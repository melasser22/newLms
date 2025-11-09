package com.ejada.subscription.repository;

import com.ejada.subscription.model.auth.SubscriptionUser;
import java.util.Optional;

public interface SubscriptionUserRepository {
  Optional<SubscriptionUser> findByLoginName(String loginName);
}
