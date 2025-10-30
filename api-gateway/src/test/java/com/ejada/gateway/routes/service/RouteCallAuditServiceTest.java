package com.ejada.gateway.routes.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ejada.gateway.routes.model.RouteCallAuditRecord;
import com.ejada.gateway.routes.repository.RouteCallAuditEntity;
import com.ejada.gateway.routes.repository.RouteCallAuditR2dbcRepository;
import io.r2dbc.spi.R2dbcException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RouteCallAuditServiceTest {

  @Mock
  private RouteCallAuditR2dbcRepository repository;

  private RouteCallAuditService service;

  @BeforeEach
  void setUp() {
    service = new RouteCallAuditService(repository);
  }

  @Test
  void recordDisablesAuditingWhenAuditTableMissing() {
    RouteCallAuditRecord record = new RouteCallAuditRecord(
        UUID.randomUUID().toString(),
        "/api/auth/admin/login",
        "POST",
        401,
        30L,
        "tenant-1",
        "corr-id",
        "127.0.0.1",
        "ON_COMPLETE",
        null);

    when(repository.save(any(RouteCallAuditEntity.class)))
        .thenReturn(Mono.error(missingAuditTableException()));

    StepVerifier.create(service.record(record)).verifyComplete();
    StepVerifier.create(service.record(record)).verifyComplete();

    verify(repository, times(1)).save(any(RouteCallAuditEntity.class));
    verifyNoMoreInteractions(repository);
  }

  private R2dbcException missingAuditTableException() {
    return new R2dbcException("relation \"route_call_audit\" does not exist", "42P01") {};
  }
}
