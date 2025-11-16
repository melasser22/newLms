package com.ejada.management.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogger.class);

  public void logTenantAction(String tenantId, String action, String details) {
    LOGGER.info("[tenant={}] action={} details={}", tenantId, action, details);
  }
}
