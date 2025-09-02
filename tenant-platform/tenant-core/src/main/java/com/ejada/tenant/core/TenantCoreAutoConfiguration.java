package com.ejada.tenant.core;

import com.ejada.tenant.core.adapters.jdbc.JdbcFeaturePolicyPort;
import com.ejada.tenant.core.adapters.jdbc.JdbcOveragePort;
import com.ejada.tenant.core.adapters.jdbc.JdbcSubscriptionPort;
import com.ejada.tenant.core.adapters.jdbc.JdbcTenantSettingsPort;
import com.ejada.tenant.core.adapters.shared.SharedFeaturePolicyPort;
import com.ejada.tenant.core.adapters.shared.SharedOveragePort;
import com.ejada.tenant.core.adapters.shared.SharedSubscriptionPort;
import com.ejada.tenant.core.adapters.shared.SharedTenantSettingsPort;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({
        SharedFeaturePolicyPort.class,
        SharedSubscriptionPort.class,
        SharedTenantSettingsPort.class,
        SharedOveragePort.class,
        JdbcFeaturePolicyPort.class,
        JdbcSubscriptionPort.class,
        JdbcTenantSettingsPort.class,
        JdbcOveragePort.class
})
public class TenantCoreAutoConfiguration {
}
