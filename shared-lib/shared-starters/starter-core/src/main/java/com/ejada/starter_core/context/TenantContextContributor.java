package com.ejada.starter_core.context;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.constants.RequestAttributeNames;
import com.ejada.common.context.ContextManager;
import com.ejada.common.context.TenantMdcUtil;
import com.ejada.starter_core.config.CoreAutoConfiguration;
import com.ejada.starter_core.tenant.TenantError;
import com.ejada.starter_core.tenant.TenantResolution;
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
        TenantResolution resolution = tenantResolver.resolve(request);
        if (resolution == null) {
            return ContextScope.noop();
        }
        if (resolution.isAbsent()) {
            return ContextScope.noop();
        }
        if (resolution.hasError()) {
            TenantError error = resolution.error();
            String headerName = properties.getHeaderName();
            if (error == null || resolution.isInvalid()) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + headerName);
            } else {
                response.sendError(error.httpStatus(), error.message());
            }
            return () -> { };
        }

        String tenant = trimToNull(resolution.tenantId());
        if (tenant == null || !TENANT_PATTERN.matcher(tenant).matches()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid " + properties.getHeaderName());
            return () -> { };
        }

        if (properties.isEchoResponseHeader()) {
            response.setHeader(properties.getHeaderName(), tenant);
        }

        request.setAttribute(RequestAttributeNames.TENANT_ID, tenant);
        request.setAttribute(properties.getHeaderName(), tenant);
        request.setAttribute(HeaderNames.X_TENANT_ID, tenant);

        MDC.put(properties.getMdcKey(), tenant);
        TenantMdcUtil.setTenantId(tenant);
        ContextManager.Tenant.Scope scope = ContextManager.Tenant.openScope(tenant);

        return () -> {
            MDC.remove(properties.getMdcKey());
            TenantMdcUtil.clear();
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
