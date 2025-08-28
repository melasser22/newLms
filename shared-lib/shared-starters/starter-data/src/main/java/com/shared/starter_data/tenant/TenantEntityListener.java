package com.shared.starter_data.tenant;

import com.common.context.ContextManager;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

/**
 * JPA entity listener that injects the current tenant into tenant-scoped entities.
 *
 * Behavior:
 * - On persist: if tenant_id is null/blank and a tenant exists in TenantContext -> set it.
 * - On update: never overwrite a non-blank tenant_id; if it was cleared accidentally and a tenant exists -> restore it.
 *
 * NOTE: Entities that want this must implement {@link TenantScoped} and
 *       register the listener via @EntityListeners(TenantEntityListener.class)
 *       (already done in TenantBaseEntity).
 */
public class TenantEntityListener {

  @PrePersist
  public void onCreate(Object entity) {
    if (!(entity instanceof TenantScoped scoped)) return;

    if (isBlank(scoped.getTenantId())) {
      String tid = ContextManager.Tenant.get();
      if (!isBlank(tid)) {
        scoped.setTenantId(tid);
      }
      // If no tenant in context, keep NULL -> visible only when allowGlobal=true
    }
  }

  @PreUpdate
  public void onUpdate(Object entity) {
    if (!(entity instanceof TenantScoped scoped)) return;

    // Do not change an existing non-blank tenantId.
    // If someone cleared it by mistake and we have a tenant in context, restore it.
    if (isBlank(scoped.getTenantId())) {
      String tid = ContextManager.Tenant.get();
      if (!isBlank(tid)) {
        scoped.setTenantId(tid);
      }
    }
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
