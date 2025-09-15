package com.ejada.starter_core.tenant;

import com.ejada.starter_core.config.CoreAutoConfiguration.CoreProps;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;

import java.util.Map;
import java.util.Objects;

public class DefaultTenantResolver implements TenantResolver {

    private final CoreProps.Tenant cfg;

    // >>> This is the constructor CoreAutoConfiguration calls <<<
    public DefaultTenantResolver(CoreProps.Tenant cfg) {
        this.cfg = cfg;
    }

    @Override
    public String resolve(HttpServletRequest request) {
        // 1) header
        String fromHeader = trimToNull(request.getHeader(cfg.getHeaderName()));
        // 2) query param
        String fromQuery = trimToNull(request.getParameter(cfg.getQueryParam()));
        // 3) jwt (optional)
        String fromJwt = null;
        if (cfg.isResolveFromJwt()) {
            fromJwt = tenantFromJwtIfAvailable(cfg.getJwtClaimNames());
        }

        String tenant = cfg.isPreferHeaderOverJwt()
                ? coalesce(fromHeader, fromQuery, fromJwt)
                : coalesce(fromJwt, fromHeader, fromQuery);

        if (tenant != null) {
            MDC.put(cfg.getMdcKey(), tenant);
        }
        return tenant;
    }

    private static String coalesce(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static String trimToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    @SuppressWarnings("unchecked")
    private static String tenantFromJwtIfAvailable(String[] claimNames) {
        try {
            Class<?> scClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = scClass.getMethod("getContext").invoke(null);
            if (context == null) return null;
            Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
            if (auth == null) return null;

            Object jwt;
            try {
                jwt = auth.getClass().getMethod("getToken").invoke(auth);
            } catch (NoSuchMethodException ex) {
                return null; // not a JwtAuthenticationToken
            }
            Object claims = jwt.getClass().getMethod("getClaims").invoke(jwt);
            if (!(claims instanceof Map)) return null;
            Map<String, Object> map = (Map<String, Object>) claims;

            for (String name : claimNames) {
                Object v = map.get(name);
                if (v != null) return Objects.toString(v, null);
            }
        } catch (Throwable ignore) { /* security not on classpath or structure differs */ }
        return null;
    }
}
