package com.shared.audit.starter.api;

import com.common.context.ContextManager;

public class DefaultTenantProvider implements TenantProvider {
  @Override
  public String getTenantId() {
    return ContextManager.Tenant.get();
  }
}