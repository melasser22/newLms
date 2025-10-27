package com.ejada.gateway.routes.service;

import com.ejada.gateway.routes.model.RouteCallAuditRecord;
import com.ejada.gateway.routes.repository.RouteCallAuditEntity;
import com.ejada.gateway.routes.repository.RouteCallAuditR2dbcRepository;
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

  private final RouteCallAuditR2dbcRepository repository;

  public RouteCallAuditService(RouteCallAuditR2dbcRepository repository) {
    this.repository = repository;
  }

  public Mono<Void> record(RouteCallAuditRecord record) {
    RouteCallAuditEntity entity = new RouteCallAuditEntity();
    entity.setCallId(UUID.randomUUID());
    entity.setRouteIdRaw(trimToNull(record.routeId()));
    entity.setRouteId(resolveRouteId(record.routeId()));
    entity.setPath(trimToNull(record.path()));
    entity.setMethod(trimToNull(record.method()));
    entity.setStatusCode(record.statusCode());
    entity.setDurationMs(record.durationMs());
    entity.setTenantId(trimToNull(record.tenantId()));
    entity.setCorrelationId(trimToNull(record.correlationId()));
    entity.setClientIp(trimToNull(record.clientIp()));
    entity.setOutcome(trimToNull(record.outcome()));
    entity.setErrorMessage(truncate(trimToNull(record.errorMessage())));
    entity.setOccurredAt(Instant.now());
    return repository.save(entity)
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)).filter(this::isTransientFailure))
        .then()
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to persist route call audit entry", ex);
          return Mono.empty();
        });
  }

  private boolean isTransientFailure(Throwable throwable) {
    return throwable instanceof R2dbcTransientResourceException
        || throwable instanceof R2dbcTimeoutException;
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

  private String truncate(String value) {
    if (value == null || value.length() <= MAX_ERROR_LENGTH) {
      return value;
    }
    return value.substring(0, MAX_ERROR_LENGTH);
  }
}

