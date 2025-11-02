package com.ejada.openapi.starter.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SharedOpenApiAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(SharedOpenApiAutoConfiguration.class));

  @Test
  void registersOpenApiAndGroupedBeansByDefault() {
    contextRunner.run(context -> {
      assertThat(context).hasSingleBean(OpenAPI.class);
      assertThat(context).hasBean("sharedGroupedOpenApi");

      GroupedOpenApi groupedOpenApi = context.getBean(GroupedOpenApi.class);
      assertThat(groupedOpenApi.getGroup()).isEqualTo("shared");
    });
  }
}

