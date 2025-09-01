package com.shared.starter_security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class JwtDecoderAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(JwtDecoderAutoConfiguration.class));

  @Test
  void missingSecretThrowsMeaningfulException() {
    contextRunner.run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("shared.security.hs256.secret");
    });
  }

  @Test
  void missingJwksUriThrowsMeaningfulException() {
    contextRunner.withPropertyValues("shared.security.mode=jwks").run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .hasCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("shared.security.jwks.uri");
    });
  }
}
