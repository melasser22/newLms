package com.ejada.subscription.repository.impl;

import com.ejada.subscription.model.auth.SubscriptionUser;
import com.ejada.subscription.properties.SubscriptionSecurityProperties;
import com.ejada.subscription.repository.SubscriptionUserRepository;
import jakarta.annotation.PostConstruct;
import java.util.Locale;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

@Repository
public class InMemorySubscriptionUserRepository implements SubscriptionUserRepository {

  private static final Logger log = LoggerFactory.getLogger(InMemorySubscriptionUserRepository.class);
  private final List<SubscriptionUser> configuredUsers;
  private final Map<String, SubscriptionUser> users = new ConcurrentHashMap<>();

  public InMemorySubscriptionUserRepository(final SubscriptionSecurityProperties properties) {
    this.configuredUsers = properties.users().stream()
        .map(user -> new SubscriptionUser(user.loginName(), user.password()))
        .toList();
  }

  @PostConstruct
  void loadUsers() {
    configuredUsers.forEach(user -> {
      String key = normalize(user.loginName());
      users.put(key, user);
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
