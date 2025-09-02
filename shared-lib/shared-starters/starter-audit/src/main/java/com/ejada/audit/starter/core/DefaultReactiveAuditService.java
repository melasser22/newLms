package com.ejada.audit.starter.core;

import com.ejada.audit.starter.api.AuditEvent;
import com.ejada.audit.starter.api.ReactiveAuditService;
import com.ejada.audit.starter.api.AuditService;
import reactor.core.publisher.Mono;
import java.util.function.UnaryOperator;

public class DefaultReactiveAuditService implements ReactiveAuditService {
  private final AuditService delegate;
  public DefaultReactiveAuditService(AuditService delegate) { this.delegate = delegate; }

  @Override public <T> Mono<T> auditMono(Mono<T> mono, UnaryOperator<AuditEvent.Builder> builderCustomizer) {
    return mono.doOnSuccess(v -> {
      AuditEvent evt = builderCustomizer.apply(AuditEvent.builder()).build();
      delegate.emit(evt);
    }).doOnError(err -> {
      AuditEvent evt = builderCustomizer.apply(AuditEvent.builder().outcome(com.ejada.audit.starter.api.AuditOutcome.FAILURE).message(err.getMessage())).build();
      delegate.emit(evt);
    });
  }
}
