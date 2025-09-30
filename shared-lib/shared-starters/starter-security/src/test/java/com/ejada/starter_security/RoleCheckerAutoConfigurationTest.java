package com.ejada.starter_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleCheckerAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SecurityAutoConfiguration.class))
      .withPropertyValues(
          "shared.security.hs256.secret=0123456789ABCDEF0123456789ABCDEF",
          "shared.security.resource-server.enabled=false");

  @Test
  void providesRoleCheckerBean() {
    contextRunner.run(context -> {
      assertTrue(context.containsBean("roleChecker"));
      assertNotNull(context.getBean(RoleChecker.class));
    });
  }
}
