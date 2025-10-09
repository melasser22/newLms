package com.ejada.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensures a qualified ObjectMapper bean is available for the API Gateway.
 * This configuration provides a fallback ObjectMapper with the "jacksonObjectMapper"
 * qualifier if one is not already defined by the shared starter library.
 */
@Configuration
public class GatewayObjectMapperConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayObjectMapperConfiguration.class);

  /**
   * Provides a fallback ObjectMapper bean with the "jacksonObjectMapper" qualifier
   * if one doesn't already exist. This ensures the error handler and other components
   * always have access to an ObjectMapper instance.
   */
  @Bean(name = "jacksonObjectMapper")
  @ConditionalOnMissingBean(name = "jacksonObjectMapper")
  public ObjectMapper jacksonObjectMapper(ObjectMapper primaryObjectMapper) {
    LOGGER.info("Registering jacksonObjectMapper qualifier for existing ObjectMapper bean");
    return primaryObjectMapper;
  }
}
