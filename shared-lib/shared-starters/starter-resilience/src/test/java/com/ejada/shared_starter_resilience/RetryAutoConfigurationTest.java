package com.ejada.shared_starter_resilience;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class RetryAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(RetryAutoConfiguration.class));

  @Test
  void enablesRetryAnnotationProcessing() {
    contextRunner.run(context -> context.getBean(RetryAutoConfiguration.class));
  }
}
