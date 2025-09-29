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
    public TenantResolution resolve(HttpServletRequest request) {
        String fromHeader = trimToNull(request.getHeader(cfg.getHeaderName()));
        String fromQuery = trimToNull(request.getParameter(cfg.getQueryParam()));
        String fromJwt = null;
        if (cfg.isResolveFromJwt()) {
            fromJwt = tenantFromJwtIfAvailable(cfg.getJwtClaimNames());
        }

        TenantResolution resolution = cfg.isPreferHeaderOverJwt()
                ? resolveInOrder(fromHeader, fromQuery, fromJwt)
                : resolveInOrder(fromJwt, fromHeader, fromQuery);

        if (resolution.hasTenant()) {
            MDC.put(cfg.getMdcKey(), resolution.tenantId());
        } else {
            MDC.remove(cfg.getMdcKey());
        }
        return resolution;
    }

    private static TenantResolution resolveInOrder(String... candidates) {
        for (String candidate : candidates) {
            TenantResolution resolution = TenantIdValidator.validate(candidate);
            if (!resolution.isAbsent()) {
                return resolution;
            }
        }
        return TenantResolution.absent();
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
