package com.ejada.config.refresh;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.EnvironmentChangeEvent;
import org.springframework.mock.env.MockEnvironment;

class ConfigRefreshAuditListenerTest {

  @Test
  void incrementsVersionAndSetsSystemProperty() {
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("gateway.subscription.enabled", "true");
    ConfigVersionTracker tracker = new ConfigVersionTracker();
    ConfigRefreshAuditListener listener = new ConfigRefreshAuditListener(environment, tracker);

    listener.onApplicationEvent(new EnvironmentChangeEvent(Collections.singleton("gateway.subscription.enabled")));

    assertThat(tracker.getCurrentVersion()).isEqualTo(2L);
    assertThat(environment.getSystemProperties()).containsEntry("app.configuration-version", "2");
  }
}
