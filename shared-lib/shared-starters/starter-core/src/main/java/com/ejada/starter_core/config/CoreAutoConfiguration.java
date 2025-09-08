package com.ejada.starter_core.config;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_core.context.ContextFilter;
import com.ejada.starter_core.logging.CorrelationIdFilter;
import com.ejada.starter_core.tenant.DefaultTenantResolver;
import com.ejada.starter_core.tenant.TenantFilter;
import com.ejada.starter_core.tenant.TenantRequirementInterceptor;
import com.ejada.starter_core.tenant.TenantResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Core Shared auto-configuration for the reactive stack.
 * Registers filters for correlation id and tenant propagation and exposes
 * optional CORS configuration.
 */
@AutoConfiguration
@EnableConfigurationProperties(CoreAutoConfiguration.CoreProps.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class CoreAutoConfiguration {

  /* -------------------------------------------
   *  Correlation / Tenant context filter
   * ------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(ContextFilter.class)
  @ConditionalOnProperty(prefix = "shared.core.context", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ContextFilter contextFilter() {
    return new ContextFilter();
  }

  /* -------------------------------------------
   *  CorrelationId filter
   * ------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(CorrelationIdFilter.class)
  @ConditionalOnProperty(prefix = "shared.core.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
  public CorrelationIdFilter correlationIdFilter(CoreProps props) {
      var c = props.getCorrelation();
      var filter = new CorrelationIdFilter(
          c.getHeaderName(),
          c.getMdcKey(),
          c.isGenerateIfMissing(),
          c.isEchoResponseHeader(),
          c.getSkipPatterns()
      );
      return filter;
  }

  /* -------------------------------------------
   *  Tenant: resolver + filter + enforcement filter
   * ------------------------------------------- */
  @Bean
  @ConditionalOnProperty(prefix = "shared.core.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
  @ConditionalOnMissingBean(TenantResolver.class)
  public TenantResolver tenantResolver(CoreProps props) {
    return new DefaultTenantResolver(props.getTenant());
  }

  @Bean
  @ConditionalOnProperty(prefix = "shared.core.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
  @ConditionalOnMissingBean(TenantFilter.class)
  public TenantFilter tenantFilter(TenantResolver resolver, CoreProps props) {
    return new TenantFilter(resolver, props.getTenant());
  }

  @Bean
  @ConditionalOnProperty(prefix = "shared.core.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
  public TenantRequirementInterceptor tenantRequirementInterceptor(CoreProps props) {
    return new TenantRequirementInterceptor(props.getTenant());
  }

  /* -------------------------------------------
   *  Optional global CORS
   * ------------------------------------------- */
  @Bean
  @ConditionalOnProperty(prefix = "shared.core.cors", name = "enabled", havingValue = "true")
  public WebFluxConfigurer sharedCorsConfigurer(CoreProps props) {
    return new WebFluxConfigurer() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        var c = props.getCors();
        registry.addMapping(c.getPathPattern())
            .allowedOrigins(c.getAllowedOrigins())
            .allowedMethods(c.getAllowedMethods())
            .allowedHeaders(c.getAllowedHeaders())
            .exposedHeaders(c.getExposedHeaders())
            .allowCredentials(c.isAllowCredentials())
            .maxAge(c.getMaxAgeSeconds());
      }
    };
  }

  /* -------------------------------------------
   *  Properties (bindable via application.yml)
   * ------------------------------------------- */
  @ConfigurationProperties("shared.core")
  public static class CoreProps {
    private final Context context = new Context();
    private final Correlation correlation = new Correlation();
    private final Tenant tenant = new Tenant();
    private final Cors cors = new Cors();

    public Context getContext() { return context; }
    public Correlation getCorrelation() { return correlation; }
    public Tenant getTenant() { return tenant; }
    public Cors getCors() { return cors; }

    public static class Context {
      private boolean enabled = true;
      private int order = Ordered.HIGHEST_PRECEDENCE + 10;
      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public int getOrder() { return order; }
      public void setOrder(int order) { this.order = order; }
    }

    public static class Correlation {
      private boolean enabled = true;
      private String headerName = HeaderNames.CORRELATION_ID;
      private String mdcKey = HeaderNames.CORRELATION_ID;
      private boolean generateIfMissing = true;
      private boolean echoResponseHeader = true;
      private int order = Ordered.HIGHEST_PRECEDENCE;
      private String[] skipPatterns = new String[]{
        "/actuator/**","/swagger-ui/**","/v3/api-docs/**","/static/**","/webjars/**","/error","/favicon.ico"
      };

      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
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
      private boolean enabled = true;
      private String headerName = HeaderNames.X_TENANT_ID;
      private String queryParam = HeaderNames.X_TENANT_ID;
      private boolean echoResponseHeader = true;
      private int order = Ordered.HIGHEST_PRECEDENCE + 20;
      private String defaultPolicy = "OPTIONAL";
      private String mdcKey = HeaderNames.X_TENANT_ID;
      private boolean resolveFromJwt = false;
      private boolean preferHeaderOverJwt = true;
      private String[] jwtClaimNames = new String[]{"tenant", "tenantId"};
      private String[] skipPatterns = new String[]{
        "/actuator/**","/swagger-ui/**","/v3/api-docs/**","/static/**","/webjars/**","/error","/favicon.ico"
      };

      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public String getHeaderName() { return headerName; }
      public void setHeaderName(String headerName) { this.headerName = headerName; }
      public String getQueryParam() { return queryParam; }
      public void setQueryParam(String queryParam) { this.queryParam = queryParam; }
      public boolean isEchoResponseHeader() { return echoResponseHeader; }
      public void setEchoResponseHeader(boolean echoResponseHeader) { this.echoResponseHeader = echoResponseHeader; }
      public int getOrder() { return order; }
      public void setOrder(int order) { this.order = order; }
      public String getDefaultPolicy() { return defaultPolicy; }
      public void setDefaultPolicy(String defaultPolicy) { this.defaultPolicy = defaultPolicy; }
      public String getMdcKey() { return mdcKey; }
      public void setMdcKey(String mdcKey) { this.mdcKey = mdcKey; }
      public boolean isResolveFromJwt() { return resolveFromJwt; }
      public void setResolveFromJwt(boolean resolveFromJwt) { this.resolveFromJwt = resolveFromJwt; }
      public boolean isPreferHeaderOverJwt() { return preferHeaderOverJwt; }
      public void setPreferHeaderOverJwt(boolean preferHeaderOverJwt) { this.preferHeaderOverJwt = preferHeaderOverJwt; }
      public String[] getJwtClaimNames() { return jwtClaimNames; }
      public void setJwtClaimNames(String[] jwtClaimNames) { this.jwtClaimNames = jwtClaimNames; }
      public String[] getSkipPatterns() { return skipPatterns; }
      public void setSkipPatterns(String[] skipPatterns) { this.skipPatterns = skipPatterns; }
    }

    public static class Cors {
      private boolean enabled = false;
      private String pathPattern = "/**";
      private String[] allowedOrigins = new String[]{"*"};
      private String[] allowedMethods = new String[]{"*"};
      private String[] allowedHeaders = new String[]{"*"};
      private String[] exposedHeaders = new String[]{};
      private boolean allowCredentials = false;
      private long maxAgeSeconds = 1800;

      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public String getPathPattern() { return pathPattern; }
      public void setPathPattern(String pathPattern) { this.pathPattern = pathPattern; }
      public String[] getAllowedOrigins() { return allowedOrigins; }
      public void setAllowedOrigins(String[] allowedOrigins) { this.allowedOrigins = allowedOrigins; }
      public String[] getAllowedMethods() { return allowedMethods; }
      public void setAllowedMethods(String[] allowedMethods) { this.allowedMethods = allowedMethods; }
      public String[] getAllowedHeaders() { return allowedHeaders; }
      public void setAllowedHeaders(String[] allowedHeaders) { this.allowedHeaders = allowedHeaders; }
      public String[] getExposedHeaders() { return exposedHeaders; }
      public void setExposedHeaders(String[] exposedHeaders) { this.exposedHeaders = exposedHeaders; }
      public boolean isAllowCredentials() { return allowCredentials; }
      public void setAllowCredentials(boolean allowCredentials) { this.allowCredentials = allowCredentials; }
      public long getMaxAgeSeconds() { return maxAgeSeconds; }
      public void setMaxAgeSeconds(long maxAgeSeconds) { this.maxAgeSeconds = maxAgeSeconds; }
    }
  }
}

