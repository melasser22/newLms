package com.shared.audit.starter.api;

import reactor.core.publisher.Mono;
import java.util.function.UnaryOperator;

/** Helper for Reactor flows. */
public interface ReactiveAuditService {
  <T> Mono<T> auditMono(Mono<T> mono, UnaryOperator<AuditEvent.Builder> builderCustomizer);
}
