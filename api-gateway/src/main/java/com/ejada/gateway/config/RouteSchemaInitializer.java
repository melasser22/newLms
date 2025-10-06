package com.ejada.gateway.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Ensures the gateway route schema exists when the application starts. This is necessary because
 * the gateway relies solely on an R2DBC connection and does not benefit from Spring's traditional
 * JDBC schema initialisation.
 */
@Component("gatewayRouteSchemaInitializer")
public class RouteSchemaInitializer implements InitializingBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteSchemaInitializer.class);
  private static final Duration INITIALISATION_TIMEOUT = Duration.ofSeconds(30);

  private final DatabaseClient databaseClient;
  private final ResourceLoader resourceLoader;
  private final AtomicBoolean initialised = new AtomicBoolean();

  public RouteSchemaInitializer(DatabaseClient databaseClient, ResourceLoader resourceLoader) {
    this.databaseClient = databaseClient;
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (!initialised.compareAndSet(false, true)) {
      return;
    }
    Resource schema = resourceLoader.getResource("classpath:schema.sql");
    if (!schema.exists()) {
      LOGGER.warn(
          "No schema.sql found on the classpath; gateway route tables will not be initialised automatically");
      return;
    }

    List<String> statements = loadStatements(schema);
    if (statements.isEmpty()) {
      LOGGER.debug("Route schema resource {} did not contain executable statements", schema);
      return;
    }

    LOGGER.info("Ensuring gateway route schema is present using {}", schema);
    Flux.fromIterable(statements)
        .concatMap(this::executeStatement)
        .then()
        .block(INITIALISATION_TIMEOUT);
  }

  private Mono<Void> executeStatement(String sql) {
    return databaseClient
        .sql(sql)
        .fetch()
        .rowsUpdated()
        .doOnError(error -> LOGGER.error("Failed to execute route schema statement: {}", sql, error))
        .onErrorResume(error -> Mono.empty())
        .then();
  }

  private List<String> loadStatements(Resource schema) throws IOException {
    try (InputStreamReader reader =
        new InputStreamReader(schema.getInputStream(), StandardCharsets.UTF_8)) {
      String content = FileCopyUtils.copyToString(reader);
      return Arrays.stream(content.split(";"))
          .map(String::trim)
          .filter(statement -> !statement.isEmpty())
          .collect(Collectors.toList());
    }
  }
}
