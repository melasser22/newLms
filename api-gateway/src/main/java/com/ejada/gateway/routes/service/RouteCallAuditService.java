package com.ejada.gateway.routes.service;

import com.ejada.gateway.routes.model.RouteCallAuditRecord;
import com.ejada.gateway.routes.repository.RouteCallAuditEntity;
import com.ejada.gateway.routes.repository.RouteCallAuditR2dbcRepository;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.R2dbcTransientResourceException;
import io.r2dbc.spi.R2dbcTimeoutException;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
public class RouteCallAuditService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteCallAuditService.class);

  private static final int MAX_ERROR_LENGTH = 2048;
  private static final String ROUTE_FOREIGN_KEY_NAME = "fk_route_call_definition";
  private static final String FOREIGN_KEY_VIOLATION_SQL_STATE = "23503";

  private final RouteCallAuditR2dbcRepository repository;

  public RouteCallAuditService(RouteCallAuditR2dbcRepository repository) {
    this.repository = repository;
  }

  public Mono<Void> record(RouteCallAuditRecord record) {
    RouteCallAuditEntity entity = new RouteCallAuditEntity();
    entity.setCallId(UUID.randomUUID());
    entity.setRouteIdRaw(trimToNull(record.routeId()));
    entity.setRouteId(resolveRouteId(record.routeId()));
    entity.setPath(defaultIfNull(trimToNull(record.path()), "unknown"));
    entity.setMethod(defaultIfNull(trimToNull(record.method()), "UNKNOWN"));
    entity.setStatusCode(record.statusCode());
    entity.setDurationMs(record.durationMs());
    entity.setTenantId(trimToNull(record.tenantId()));
    entity.setCorrelationId(trimToNull(record.correlationId()));
    entity.setClientIp(trimToNull(record.clientIp()));
    entity.setOutcome(defaultIfNull(trimToNull(record.outcome()), "UNKNOWN"));
    entity.setErrorMessage(truncate(trimToNull(record.errorMessage())));
    entity.setOccurredAt(Instant.now());
    return saveWithRetry(entity)
        .then()
        .onErrorResume(ex -> handlePersistenceFailure(entity, ex));
  }

  private Mono<RouteCallAuditEntity> saveWithRetry(RouteCallAuditEntity entity) {
    return repository
        .save(entity)
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)).filter(this::isTransientFailure));
  }

  private Mono<Void> handlePersistenceFailure(RouteCallAuditEntity entity, Throwable throwable) {
    if (entity.getRouteId() != null && isForeignKeyViolation(throwable)) {
      LOGGER.debug(
          "Route definition {} not found; persisting audit entry without foreign key link",
          entity.getRouteIdRaw());
      entity.setRouteId(null);
      return saveWithRetry(entity)
          .then()
          .onErrorResume(fallbackError -> {
            LOGGER.warn("Failed to persist route call audit entry", fallbackError);
            return Mono.empty();
          });
    }
    LOGGER.warn("Failed to persist route call audit entry", throwable);
    return Mono.empty();
  }

  private boolean isTransientFailure(Throwable throwable) {
    return throwable instanceof R2dbcTransientResourceException
        || throwable instanceof R2dbcTimeoutException;
  }

  private boolean isForeignKeyViolation(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof R2dbcException r2dbcException
          && FOREIGN_KEY_VIOLATION_SQL_STATE.equals(r2dbcException.getSqlState())) {
        return true;
      }
      String message = current.getMessage();
      if (message != null && message.contains(ROUTE_FOREIGN_KEY_NAME)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private UUID resolveRouteId(String routeId) {
    if (!StringUtils.hasText(routeId)) {
      return null;
    }
    try {
      return UUID.fromString(routeId.trim());
    } catch (IllegalArgumentException ex) {
      LOGGER.debug("Route id {} is not a UUID and will not be linked to definition table", routeId);
      return null;
    }
  }

  private String trimToNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private String defaultIfNull(String value, String defaultValue) {
    return value != null ? value : defaultValue;
  }

  private String truncate(String value) {
    if (value == null || value.length() <= MAX_ERROR_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_LENGTH);
  }
}

