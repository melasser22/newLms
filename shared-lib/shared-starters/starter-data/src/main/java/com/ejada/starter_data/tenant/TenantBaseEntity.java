package com.ejada.starter_data.tenant;

import com.ejada.starter_data.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Mapped superclass for tenant-scoped entities.
 *
 * - Adds tenant_id column.
 * - Declares a Hibernate filter "tenantFilter" using two params:
 *     tenantId     : String   (current tenant from TenantContext)
 *     allowGlobal  : boolean  (include rows with tenant_id IS NULL)
 *
 * NOTE:
 * - Keep tenant_id nullable so "global" rows are possible.
 * - We mark tenant_id updatable=false to prevent moving records across tenants.
 * - Concrete @Entity types should extend this class.
 */
@MappedSuperclass
@EntityListeners(TenantEntityListener.class)
@FilterDef(
    name = "tenantFilter",
    parameters = {
        // Hibernate 6: Class-based types (preferred)
        @ParamDef(name = "tenantId",    type = String.class),
        @ParamDef(name = "allowGlobal", type = Boolean.class)

        // If you ever downgrade to Hibernate 5.x, switch to:
        // @ParamDef(name = "tenantId",    type = "string"),
        // @ParamDef(name = "allowGlobal", type = "boolean")
    }
)
@Filter(
    name = "tenantFilter",
    condition = "(tenant_id = :tenantId OR (:allowGlobal = true AND tenant_id IS NULL))"
)
public abstract class TenantBaseEntity extends BaseEntity implements TenantScoped {

  @Column(name = "tenant_id", length = 64, updatable = false) // nullable by design
  private String tenantId;

  @Override public String getTenantId() { return tenantId; }
  @Override public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
