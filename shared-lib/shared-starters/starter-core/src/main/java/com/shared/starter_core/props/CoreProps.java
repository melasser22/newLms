package com.shared.starter_core.props;

import com.shared.common.BaseStarterProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

// If you already have this constant elsewhere, keep using it.
// Otherwise create the simple HeaderNames class shown below.
import com.common.constants.HeaderNames;

@Getter
@ConfigurationProperties(prefix = "shared.core")
public class CoreProps implements BaseStarterProperties {

    private final Correlation correlation = new Correlation();
    private final Tenant tenant = new Tenant();

    @Getter
    @Setter
    public static class Correlation {
        /** HTTP header name used for correlation id */
        private String headerName = HeaderNames.CORRELATION_ID; // "X-Correlation-Id"

        /** MDC key to store correlation id under */
        private String mdcKey = HeaderNames.CORRELATION_ID;

        /** Generate a UUID if the client did not send a correlation id */
        private boolean generateIfMissing = true;

        /** Echo correlation id back in the response header */
        private boolean echoResponseHeader = true;

        /** Filter order (run first so others can use the id) */
        private int order = Ordered.HIGHEST_PRECEDENCE + 5; // -2147483643

        /** URL patterns to skip */
        private String[] skipPatterns = new String[] {
                "/actuator/**", "/swagger-ui/**", "/v3/api-docs/**",
                "/static/**", "/webjars/**", "/error", "/favicon.ico"
        };
    }

    @Getter
    @Setter
    public static class Tenant {
        /** Enable tenant resolution + enforcement */
        private boolean enabled = true;

        /** Header and query parameter names */
        private String headerName = HeaderNames.X_TENANT_ID;   // "x_tenant_id"
        private String queryParam = "tenantId";

        /** MDC key for logs */
        private String mdcKey = HeaderNames.X_TENANT_ID;

        /** Echo back the tenant in response header */
        private boolean echoResponseHeader = true;

        /** Resolve from JWT if available (optional) */
        private boolean resolveFromJwt = true;

        /** Candidate claim names in JWT */
        private String[] jwtClaimNames = new String[]{"tenant", "tenant_id", "tid"};

        /** Resolution preference: header > query > jwt */
        private boolean preferHeaderOverJwt = true;

        /** Interceptor default policy if no annotation found: OPTIONAL or REQUIRED */
        private String defaultPolicy = "OPTIONAL";

        /** Filter order (after correlation, but still very early) */
        private int order = Ordered.HIGHEST_PRECEDENCE + 10;

        /** Skip patterns */
        private String[] skipPatterns = new String[]{
            "/actuator/**","/swagger-ui/**","/v3/api-docs/**",
            "/static/**","/webjars/**","/error","/favicon.ico"
        };
    }
}
