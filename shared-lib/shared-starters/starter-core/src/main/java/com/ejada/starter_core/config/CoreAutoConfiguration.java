package com.ejada.starter_core.config;

import com.ejada.common.constants.HeaderNames;
import com.ejada.starter_core.context.ContextFilter;
import com.ejada.starter_core.logging.CorrelationIdFilter;
import com.ejada.starter_core.tenant.DefaultTenantResolver;
import com.ejada.starter_core.tenant.TenantFilter;
import com.ejada.starter_core.tenant.TenantRequirementInterceptor;
import com.ejada.starter_core.tenant.TenantResolver;
import com.ejada.starter_core.web.FilterSkipUtils;

import jakarta.servlet.Filter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Core Shared auto-configuration:
 *  - Registers ContextFilter (correlation/tenant propagation)
 *  - Registers CorrelationIdFilter (independent correlation id)
 *  - Registers Tenant resolver + filter + enforcement interceptor
 *  - Optional global CORS
 *
 * Keep JacksonConfig, ValidationConfig, LoggingAutoConfiguration in separate classes.
 */
@AutoConfiguration
@EnableConfigurationProperties(CoreAutoConfiguration.CoreProps.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class CoreAutoConfiguration {

  /* -------------------------------------------
   *  Context (correlation + tenant) filter
   *  - provide the Filter bean if missing
   *  - register it via FilterRegistrationBean under a DIFFERENT bean name
   * ------------------------------------------- */
  @Bean
  @ConditionalOnClass(Filter.class)
  @ConditionalOnMissingBean(ContextFilter.class)
  @ConditionalOnProperty(prefix = "shared.core.context", name = "enabled", havingValue = "true", matchIfMissing = true)
  public ContextFilter contextFilterBean(ObjectProvider<TenantResolver> tenantResolver) {
    return new ContextFilter(tenantResolver.getIfAvailable(TenantResolver::noop));
  }

  @Bean(name = "contextFilterRegistration")
  @ConditionalOnClass(Filter.class)
  @ConditionalOnMissingBean(name = "contextFilterRegistration")
  @ConditionalOnProperty(prefix = "shared.core.context", name = "enabled", havingValue = "true", matchIfMissing = true)
  public FilterRegistrationBean<ContextFilter> contextFilterRegistration(ContextFilter filter, CoreProps props) {
    FilterRegistrationBean<ContextFilter> reg = new FilterRegistrationBean<>(filter);
    reg.addUrlPatterns("/*");
    reg.setOrder(props.getContext().getOrder());
    reg.setName("contextFilter");
    return reg;
  }

  /* -------------------------------------------
   *  CorrelationId filter
   *  - same pattern: bean + registration bean
   * ------------------------------------------- */
  @Bean
  @ConditionalOnMissingBean(CorrelationIdFilter.class)
  @ConditionalOnProperty(prefix = "shared.core.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
  public CorrelationIdFilter correlationIdFilterBean(CoreProps props) {
      var c = props.getCorrelation();
      return new CorrelationIdFilter(
          c.getHeaderName(),
          c.getMdcKey(),
          c.isGenerateIfMissing(),
          c.isEchoResponseHeader(),
          c.getSkipPatterns()
      );
  }

  @Bean(name = "correlationIdFilterRegistration")
  @ConditionalOnMissingBean(name = "correlationIdFilterRegistration")
  @ConditionalOnProperty(prefix = "shared.core.correlation", name = "enabled", havingValue = "true", matchIfMissing = true)
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter, CoreProps props) {
      var reg = new FilterRegistrationBean<>(filter);
      reg.setOrder(props.getCorrelation().getOrder()); // Highest precedence (run before context/tenant)
      reg.setName("correlationIdFilter");
      reg.addUrlPatterns("/*");
      return reg;
  }

  /* -------------------------------------------
   *  Tenant: resolver + filter + enforcement interceptor
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

  @Bean(name = "tenantFilterRegistration")
  @ConditionalOnProperty(prefix = "shared.core.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
  @ConditionalOnMissingBean(name = "tenantFilterRegistration")
  public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter, CoreProps props) {
    var reg = new FilterRegistrationBean<>(filter);
    reg.setOrder(props.getTenant().getOrder()); // after correlation, before most others
    reg.setName("tenantFilter");
    reg.addUrlPatterns("/*");
    return reg;
  }

  /** Registers the @RequireTenant/@OptionalTenant enforcement (400 if missing). */
  @Bean
  @ConditionalOnProperty(prefix = "shared.core.tenant", name = "enabled", havingValue = "true", matchIfMissing = true)
  public WebMvcConfigurer tenantRequirementConfigurer(CoreProps props) {
    return new WebMvcConfigurer() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TenantRequirementInterceptor(props.getTenant()))
                .order(props.getTenant().getOrder() + 1);
      }
    };
  }

  /* -------------------------------------------
   *  Optional global CORS
   *  (Don’t use @ConditionalOnMissingBean(WebMvcConfigurer) so it can co-exist with the interceptor config)
   * ------------------------------------------- */
  @Bean
  @ConditionalOnProperty(prefix = "shared.core.cors", name = "enabled", havingValue = "true")
  public WebMvcConfigurer sharedCorsConfigurer(CoreProps props) {
    return new WebMvcConfigurer() {
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

    /** Controls ContextFilter registration (correlation + tenant). */
    public static class Context {
      /** Enable/disable ContextFilter */
      private boolean enabled = true;
      /** Filter order (run after correlation by default) */
      private int order = Integer.MIN_VALUE + 10;

      public boolean isEnabled() { return enabled; }
      public void setEnabled(boolean enabled) { this.enabled = enabled; }
      public int getOrder() { return order; }
      public void setOrder(int order) { this.order = order; }
    }

    /** Controls CorrelationIdFilter registration. */
    public static class Correlation {
      private boolean enabled = true;
      private String headerName = HeaderNames.CORRELATION_ID; // "X-Correlation-Id"
      private String mdcKey = HeaderNames.CORRELATION_ID;
      private boolean generateIfMissing = true;
      private boolean echoResponseHeader = true;
      private int order = Integer.MIN_VALUE + 5; // effectively Ordered.HIGHEST_PRECEDENCE
      private String[] skipPatterns = FilterSkipUtils.defaultPatterns();

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

    /** Controls Tenant resolution + enforcement. */
    public static class Tenant {
      /** Enable tenant pipeline */
      private boolean enabled = true;

      /** Header and query parameter names */
      private String headerName = HeaderNames.X_TENANT_ID;   // "X-Tenant-Id"
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
      private int order = Integer.MIN_VALUE + 10;

      /** Skip patterns */
      private String[] skipPatterns = FilterSkipUtils.defaultPatterns();

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

    /** Global CORS options. */
    public static class Cors {
      private boolean enabled = false;
      private String pathPattern = "/**";
      private String[] allowedOrigins = new String[]{"*"};
      private String[] allowedMethods = new String[]{"GET","POST","PUT","PATCH","DELETE","OPTIONS"};
      private String[] allowedHeaders = new String[]{"*"};
      private String[] exposedHeaders = new String[]{HeaderNames.CORRELATION_ID, HeaderNames.X_TENANT_ID};
      private boolean allowCredentials = false;
      private long maxAgeSeconds = 3600;

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
