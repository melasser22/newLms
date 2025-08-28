package com.shared.audit.starter.core;

import com.shared.audit.starter.api.AuditEvent;
import com.shared.audit.starter.api.ReactiveAuditService;
import com.shared.audit.starter.api.AuditService;
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
      AuditEvent evt = builderCustomizer.apply(AuditEvent.builder().outcome(com.shared.audit.starter.api.AuditOutcome.FAILURE).message(err.getMessage())).build();
      delegate.emit(evt);
    });
  }
}
