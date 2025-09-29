package com.ejada.starter_core.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.tenant.TenantResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.io.IOException;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Tenant contributor responsible for resolving tenant identifiers and exposing
 * them via {@link ContextManager} as well as MDC entries.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextContributor implements RequestContextContributor {

    private static final Pattern TENANT_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,36}");

    private final TenantResolver tenantResolver;
    private final CoreAutoConfiguration.CoreProps.Tenant properties;

    public TenantContextContributor(TenantResolver tenantResolver,
                                    CoreAutoConfiguration.CoreProps.Tenant properties) {
        this.tenantResolver = Objects.requireNonNull(tenantResolver, "tenantResolver");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    @Override
    public ContextScope contribute(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String tenant = trimToNull(tenantResolver.resolve(request));
        if (tenant == null) {
            return ContextScope.noop();
        }
        if (!TENANT_PATTERN.matcher(tenant).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + HeaderNames.X_TENANT_ID);
            return () -> { };
        }

        if (properties.isEchoResponseHeader()) {
            response.setHeader(properties.getHeaderName(), tenant);
        }

        MDC.put(properties.getMdcKey(), tenant);
        MDC.put(HeaderNames.X_TENANT_ID, tenant);
        ContextManager.Tenant.Scope scope = ContextManager.Tenant.openScope(tenant);

        return () -> {
            MDC.remove(properties.getMdcKey());
            MDC.remove(HeaderNames.X_TENANT_ID);
            scope.close();
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
