package com.ejada.starter_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleCheckerAutoConfigurationTest {

  private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
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

  @Test
  void providesAuthorizationExpressionsBean() {
    contextRunner.run(context -> {
      assertTrue(context.containsBean("authorizationExpressions"));
      assertNotNull(context.getBean(com.ejada.starter_security.authorization.AuthorizationExpressions.class));
    });
  }
}
