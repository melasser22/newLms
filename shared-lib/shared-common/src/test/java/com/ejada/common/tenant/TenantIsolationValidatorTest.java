package com.ejada.common.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.common.context.ContextManager;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantIsolationValidatorTest {

    @AfterEach
    void clearContext() {
        ContextManager.Tenant.clear();
    }

    @Test
    void requireTenantNormalisesAndReturnsValue() {
        ContextManager.Tenant.set("  TENANT-A  ");

        String tenant = TenantIsolationValidator.requireTenant("test-operation");

        assertThat(tenant).isEqualTo("tenant-a");
    }

    @Test
    void requireTenantThrowsWhenMissing() {
        ContextManager.Tenant.clear();

        assertThatThrownBy(() -> TenantIsolationValidator.requireTenant("missing"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tenant context is required");
    }

    @Test
    void requireTenantUuidValidatesFormat() {
        UUID tenantId = UUID.randomUUID();
        ContextManager.Tenant.set(tenantId.toString());

        assertThat(TenantIsolationValidator.requireTenantUuid("uuid-op")).isEqualTo(tenantId);

        ContextManager.Tenant.set("not-a-uuid");
        assertThatThrownBy(() -> TenantIsolationValidator.requireTenantUuid("uuid-op"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not a valid UUID");
    }

    @Test
    void currentTenantOrPublicFallsBackWhenMissing() {
        ContextManager.Tenant.clear();
        assertThat(TenantIsolationValidator.currentTenantOrPublic()).isEqualTo("public");

        ContextManager.Tenant.set("tenant-b");
        assertThat(TenantIsolationValidator.currentTenantOrPublic()).isEqualTo("tenant-b");
    }

    @Test
    void verifyRedisOperationRequiresTenant() {
        assertThatThrownBy(() -> TenantIsolationValidator.verifyRedisOperation("redis", null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Tenant context is required");
        assertThatThrownBy(() -> TenantIsolationValidator.verifyRedisOperation("redis", " "))
            .isInstanceOf(IllegalStateException.class);

        TenantIsolationValidator.verifyRedisOperation("redis", "tenant-c");
    }

    @Test
    void verifyKafkaOperationRequiresTenant() {
        assertThatThrownBy(() -> TenantIsolationValidator.verifyKafkaOperation("kafka", ""))
            .isInstanceOf(IllegalStateException.class);

        TenantIsolationValidator.verifyKafkaOperation("kafka", "tenant-d");
    }

    @Test
    void verifyDatabaseAccessRequiresNonNullUuid() {
        UUID tenantId = UUID.randomUUID();
        TenantIsolationValidator.verifyDatabaseAccess("db", tenantId);

        assertThatThrownBy(() -> TenantIsolationValidator.verifyDatabaseAccess("db", null))
            .isInstanceOf(NullPointerException.class);
    }
}
