package com.ejada.audit.starter.api;

import com.ejada.common.context.ContextManager;


public class DefaultTenantProvider implements TenantProvider {
  @Override
  public String getTenantId() {
    return ContextManager.Tenant.get();
  }
}