package com.ejada.audit.starter.core.dispatch.sinks;

import com.ejada.audit.starter.api.AuditEvent;

public interface Sink {
  void send(AuditEvent event) throws Exception;
  default String name() { return getClass().getSimpleName(); }
}
