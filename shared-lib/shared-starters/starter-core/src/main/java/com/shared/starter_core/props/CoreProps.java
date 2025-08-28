package com.shared.starter_core.props;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

// If you already have this constant elsewhere, keep using it.
// Otherwise create the simple HeaderNames class shown below.
import com.common.constants.HeaderNames;

@ConfigurationProperties(prefix = "shared.core")
public class CoreProps {

    private final Correlation correlation = new Correlation();

    public Correlation getCorrelation() {
        return correlation;
    }
    private final Tenant tenant = new Tenant();

    public Tenant getTenant() { return tenant; }

    public static class Correlation {
        /** HTTP header name used for correlation id */
        private String headerName = HeaderNames.CORRELATION_ID; // "X-Correlation-Id"

        /** MDC key to store correlation id under */
        private String mdcKey = "correlationId";

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

        // --- getters/setters ---

        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }

        public String getMdcKey() { return mdcKey; }
        public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }

        public boolean isGenerateIfMissing() { return generateIfMissing; }
        public void setGenerateIfMissing(boolean generateIfMissing) { this.generateIfMissing = generateIfMissing; }

        public boolean isEchoResponseHeader() { return echoResponseHeader; }
        public void setEchoResponseHeader(boolean echoResponseHeader) { this.echoResponseHeader = echoResponseHeader; }

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }

        public String[] getSkipPatterns() { return skipPatterns; }
        public void setSkipPatterns(String[] skipPatterns) { this.skipPatterns = skipPatterns; }
    }

    public static class Tenant {
        /** Enable tenant resolution + enforcement */
        private boolean enabled = true;

        /** Header and query parameter names */
        private String headerName = HeaderNames.TENANT_ID;   // "X-Tenant-Id"
        private String queryParam = "tenantId";

        /** MDC key for logs */
        private String mdcKey = "tenantId";

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

        // getters/setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public String getHeaderName() { return headerName; }
        public void setHeaderName(String headerName) { this.headerName = headerName; }

        public String getQueryParam() { return queryParam; }
        public void setQueryParam(String queryParam) { this.queryParam = queryParam; }

        public String getMdcKey() { return mdcKey; }
        public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }

        public boolean isEchoResponseHeader() { return echoResponseHeader; }
        public void setEchoResponseHeader(boolean echoResponseHeader) { this.echoResponseHeader = echoResponseHeader; }

        public boolean isResolveFromJwt() { return resolveFromJwt; }
        public void setResolveFromJwt(boolean resolveFromJwt) { this.resolveFromJwt = resolveFromJwt; }

        public String[] getJwtClaimNames() { return jwtClaimNames; }
        public void setJwtClaimNames(String[] jwtClaimNames) { this.jwtClaimNames = jwtClaimNames; }

        public boolean isPreferHeaderOverJwt() { return preferHeaderOverJwt; }
        public void setPreferHeaderOverJwt(boolean preferHeaderOverJwt) { this.preferHeaderOverJwt = preferHeaderOverJwt; }

        public String getDefaultPolicy() { return defaultPolicy; }
        public void setDefaultPolicy(String defaultPolicy) { this.defaultPolicy = defaultPolicy; }

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }

        public String[] getSkipPatterns() { return skipPatterns; }
        public void setSkipPatterns(String[] skipPatterns) { this.skipPatterns = skipPatterns; }
    }
}
