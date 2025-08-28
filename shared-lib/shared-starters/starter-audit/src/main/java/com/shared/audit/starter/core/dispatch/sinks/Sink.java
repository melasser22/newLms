package com.shared.audit.starter.core.dispatch.sinks;

import com.shared.audit.starter.api.AuditEvent;

public interface Sink {
  void send(AuditEvent event) throws Exception;
  default String name() { return getClass().getSimpleName(); }
}
