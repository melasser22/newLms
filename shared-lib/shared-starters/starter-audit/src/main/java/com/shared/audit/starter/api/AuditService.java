package com.shared.audit.starter.api;

public interface AuditService {
  void emit(AuditEvent event);
}
