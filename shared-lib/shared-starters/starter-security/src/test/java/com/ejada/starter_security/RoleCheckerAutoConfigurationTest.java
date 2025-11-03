package com.ejada.starter_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

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

  @Test
  void preAuthorizeCanResolveAuthorizationExpressionsBean() {
    contextRunner
        .withUserConfiguration(SecuredServiceConfig.class)
        .run(context -> {
          var service = context.getBean(SecuredService.class);

          var authentication = new TestingAuthenticationToken("user", "password", "ROLE_EJADA_OFFICER");
          SecurityContextHolder.getContext().setAuthentication(authentication);
          try {
            service.securedOperation();
          } finally {
            SecurityContextHolder.clearContext();
          }
        });
  }

  @Configuration(proxyBeanMethods = false)
  static class SecuredServiceConfig {
    @Bean
    SecuredService securedService() {
      return new SecuredService();
    }
  }

  static class SecuredService {
    @PreAuthorize("@authorizationExpressions.isEjadaOfficer(authentication)")
    void securedOperation() {
      // no-op
    }
  }
}
