package com.ejada.starter_security;

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
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("shared.security.hs256.secret");
    });
  }

  @Test
  void missingJwksUriThrowsMeaningfulException() {
    contextRunner.withPropertyValues("shared.security.mode=jwks").run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("shared.security.jwks.uri");
    });
  }

  @Test
  void invalidBase64SecretIsRejected() {
    contextRunner.withPropertyValues("shared.security.hs256.secret=not-base64!!!").run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure())
          .hasRootCauseInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Base64-encoded");
    });
  }

  @Test
  void shortDecodedSecretIsRejected() {
    contextRunner
        .withPropertyValues("shared.security.hs256.secret=dG9vLXNob3J0")
        .run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure())
              .hasRootCauseInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("at least 32 bytes");
        });
  }
}
