package com.ejada.gateway.config;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Provides an in-memory R2DBC {@link ConnectionFactory} when no external database configuration
 * is supplied. This allows the gateway to start for local development scenarios even when the
 * Config Server is unavailable.
 */
@Configuration
@ConditionalOnClass(ConnectionFactory.class)
public class R2dbcFallbackConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(R2dbcFallbackConfiguration.class);

  private static final String FALLBACK_URL =
      "r2dbc:h2:mem:///gateway-routes?options=DB_CLOSE_DELAY=-1;MODE=PostgreSQL";

  @Bean
  @ConditionalOnMissingBean(ConnectionFactory.class)
  public ConnectionFactory inMemoryGatewayConnectionFactory() {
    LOGGER.warn(
        "spring.r2dbc.url is not configured; falling back to an in-memory H2 database at {}. "
            + "Provide SPRING_R2DBC_URL (or spring.r2dbc.url) to connect to a persistent store.",
        FALLBACK_URL);
    return ConnectionFactories.get(FALLBACK_URL);
  }

  @Bean
  @ConditionalOnMissingBean(name = "gatewayRouteSchemaInitializer")
  public ConnectionFactoryInitializer gatewayRouteSchemaInitializer(
      ConnectionFactory inMemoryGatewayConnectionFactory,
      ResourceLoader resourceLoader) {
    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(inMemoryGatewayConnectionFactory);

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
