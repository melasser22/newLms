package com.ejada.gateway.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures that the shared {@code ObjectMapper} bean remains the single primary
 * mapper by downgrading Spring Boot's default {@code jacksonObjectMapper}
 * definition. This prevents duplicate primary ObjectMapper beans when the
 * shared starter is present alongside Spring WebFlux auto-configuration.
 */
@Configuration
public class JacksonPrimaryAdjustmentConfiguration {

  @Bean
  public static BeanFactoryPostProcessor jacksonPrimaryReconciler() {
    return beanFactory -> {
      if (beanFactory.containsBeanDefinition("jacksonObjectMapper")) {
        beanFactory.getBeanDefinition("jacksonObjectMapper").setPrimary(false);
      }
    };
  }
}
