package com.ejada.gateway.config;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
@Component
public class RouteSchemaInitializer implements ApplicationRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteSchemaInitializer.class);
  private static final Duration INITIALISATION_TIMEOUT = Duration.ofSeconds(30);

  private final DatabaseClient databaseClient;
  private final ResourceLoader resourceLoader;
  private final Mono<Void> schemaInitialisation;

  public RouteSchemaInitializer(DatabaseClient databaseClient, ResourceLoader resourceLoader) {
    this.databaseClient = databaseClient;
    this.resourceLoader = resourceLoader;
    this.schemaInitialisation = Mono.defer(this::initialiseSchema).cache();
  }

  /** Ensures the gateway route schema exists, executing the scripts at most once. */
  public Mono<Void> ensureSchema() {
    return schemaInitialisation;
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    ensureSchema().block(INITIALISATION_TIMEOUT);
  }

  private Mono<Void> initialiseSchema() {
    Resource schema = resourceLoader.getResource("classpath:schema.sql");
    if (!schema.exists()) {
      LOGGER.warn(
          "No schema.sql found on the classpath; gateway route tables will not be initialised automatically");
      return Mono.empty();
    }

    List<String> statements;
    try {
      statements = loadStatements(schema);
    } catch (IOException ex) {
      LOGGER.error("Failed to read route schema resource {}", schema, ex);
      return Mono.error(ex);
    }

    if (statements.isEmpty()) {
      LOGGER.debug("Route schema resource {} did not contain executable statements", schema);
      return Mono.empty();
    }

    LOGGER.info("Ensuring gateway route schema is present using {}", schema);
    return Flux.fromIterable(statements)
        .concatMap(this::executeStatement)
        .then();
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
