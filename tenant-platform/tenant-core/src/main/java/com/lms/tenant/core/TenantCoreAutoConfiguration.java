package com.lms.tenant.core;

import com.lms.tenant.core.adapters.jdbc.JdbcFeaturePolicyPort;
import com.lms.tenant.core.adapters.jdbc.JdbcOveragePort;
import com.lms.tenant.core.adapters.jdbc.JdbcSubscriptionPort;
import com.lms.tenant.core.adapters.jdbc.JdbcTenantSettingsPort;
import com.lms.tenant.core.adapters.shared.SharedFeaturePolicyPort;
import com.lms.tenant.core.adapters.shared.SharedOveragePort;
import com.lms.tenant.core.adapters.shared.SharedSubscriptionPort;
import com.lms.tenant.core.adapters.shared.SharedTenantSettingsPort;
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
