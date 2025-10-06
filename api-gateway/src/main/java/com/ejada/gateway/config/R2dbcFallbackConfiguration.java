package com.ejada.gateway.config;

import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Provides an in-memory R2DBC {@link ConnectionFactory} when no external database configuration
 * is supplied. This allows the gateway to start for local development scenarios even when the
 * Config Server is unavailable.
 */
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class R2dbcFallbackConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(R2dbcFallbackConfiguration.class);

  private static final String FALLBACK_URL = "r2dbc:postgresql://postgres:5432/lms";

  @Bean
  @ConditionalOnMissingBean(ConnectionFactory.class)
  public ConnectionFactory gatewayConnectionFactory(R2dbcProperties properties) {
    if (StringUtils.hasText(properties.getUrl())) {
      LOGGER.info(
          "spring.r2dbc.url detected ({}); configuring persistent route store connection.",
          properties.getUrl());
      ConnectionFactoryOptions.Builder builder =
          ConnectionFactoryOptions.builder().from(ConnectionFactoryOptions.parse(properties.getUrl()));
      if (StringUtils.hasText(properties.getUsername())) {
        builder.option(USER, properties.getUsername());
      }
      if (properties.getPassword() != null) {
        builder.option(PASSWORD, properties.getPassword());
      }
      if (StringUtils.hasText(properties.getName())) {
        builder.option(ConnectionFactoryOptions.DATABASE, properties.getName());
      }
      if (!CollectionUtils.isEmpty(properties.getProperties())) {
        properties.getProperties()
            .forEach((key, value) -> builder.option(Option.valueOf(key), value));
      }
      return ConnectionFactories.get(builder.build());
    }

    LOGGER.warn(
        "spring.r2dbc.url is not configured; falling back to default PostgreSQL connection {}. "
            + "Provide SPRING_R2DBC_URL (or spring.r2dbc.url) to target the correct database.",
        FALLBACK_URL);
    return ConnectionFactories.get(FALLBACK_URL);
  }

  @Bean
  @ConditionalOnMissingBean(name = "gatewayRouteSchemaInitializer")
  public ConnectionFactoryInitializer gatewayRouteSchemaInitializer(
      ConnectionFactory connectionFactory,
      ResourceLoader resourceLoader) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);

    Resource schema = resourceLoader.getResource("classpath:schema.sql");
    if (schema.exists()) {
      ResourceDatabasePopulator populator = new ResourceDatabasePopulator(schema);
      populator.setContinueOnError(true);
      initializer.setDatabasePopulator(populator);
      LOGGER.info("Initialising in-memory gateway route schema from {}", schema);
    } else {
      LOGGER.warn("No schema.sql found on the classpath; gateway route tables will not be initialised");
    }

    return initializer;
  }
}
