package com.ejada.audit.starter.api;

public interface AuditService {
  void emit(AuditEvent event);
}
