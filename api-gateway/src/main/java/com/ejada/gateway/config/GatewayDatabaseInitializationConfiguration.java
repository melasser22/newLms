package com.ejada.gateway.config;

import io.r2dbc.spi.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

/**
 * Ensures the gateway R2DBC schema is applied before the reactive repositories are used.
 *
 * <p>The initializer honours {@code spring.sql.init.mode}; setting it to {@code never} disables
 * the schema bootstrap while keeping the default behaviour enabled for local and production
 * deployments that do not manage the schema externally.</p>
 */
@Configuration
@ConditionalOnExpression("!'${spring.sql.init.mode:always}'.equalsIgnoreCase('never')")
public class GatewayDatabaseInitializationConfiguration {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(GatewayDatabaseInitializationConfiguration.class);

  @Bean
  ConnectionFactoryInitializer gatewayConnectionFactoryInitializer(
      ConnectionFactory connectionFactory) {
    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setContinueOnError(true);
    populator.setIgnoreFailedDrops(true);
    populator.addScript(new ClassPathResource("schema.sql"));

    ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
    initializer.setConnectionFactory(connectionFactory);
    initializer.setDatabasePopulator(populator);

    LOGGER.info("Gateway schema initialiser configured (spring.sql.init.mode != never)");
    return initializer;
  }
}

