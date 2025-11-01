package com.ejada.sec.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class OpenLoginSecurityConfigTest {

  @Test
  void loginMatchersExcludeDeprecatedGatewayPrefixedAuthPaths() throws Exception {
    Field field = OpenLoginSecurityConfig.class.getDeclaredField("LOGIN_MATCHERS");
    field.setAccessible(true);
    String[] matchers = (String[]) field.get(null);

    assertThat(matchers)
        .as("login security matcher should no longer include deprecated prefixed gateway paths")
        .doesNotContain("/sec/api/v1/auth/**");
  }
}
