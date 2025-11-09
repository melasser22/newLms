package com.ejada.subscription.repository.impl;

import com.ejada.subscription.model.auth.SubscriptionUser;
import com.ejada.subscription.properties.SubscriptionSecurityProperties;
import com.ejada.subscription.repository.SubscriptionUserRepository;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySubscriptionUserRepository implements SubscriptionUserRepository {

  private static final Logger log = LoggerFactory.getLogger(InMemorySubscriptionUserRepository.class);
  private final SubscriptionSecurityProperties properties;
  private final Map<String, SubscriptionUser> users = new ConcurrentHashMap<>();

  public InMemorySubscriptionUserRepository(final SubscriptionSecurityProperties properties) {
    this.properties = properties;
  }

  @PostConstruct
  void loadUsers() {
    properties.getUsers().forEach(user -> {
      String key = normalize(user.getLoginName());
      users.put(key, new SubscriptionUser(user.getLoginName(), user.getPassword()));
    });
    log.info("Loaded {} subscription authentication users", users.size());
  }

  @Override
  public Optional<SubscriptionUser> findByLoginName(final String loginName) {
    if (loginName == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(users.get(normalize(loginName)));
  }

  private String normalize(final String loginName) {
    return loginName.toLowerCase(Locale.ROOT);
  }
}
