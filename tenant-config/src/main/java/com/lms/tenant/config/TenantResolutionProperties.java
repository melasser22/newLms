package com.lms.tenant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lms.tenant.resolution")
public class TenantResolutionProperties {
  /** If true, try to parse subdomain as tenant key. */
  private boolean useSubdomain = true;
  /** Header names checked in order when subdomain not resolved. */
  private String headerPrimary = "X-Tenant-ID";
  private String headerSecondary = "X-Auth-Tenant";
  /** Enable DataSource wrapping to set app.current_tenant on every borrowed connection. */
  private boolean wrapDataSource = true;

  public boolean isUseSubdomain() { return useSubdomain; }
  public void setUseSubdomain(boolean useSubdomain) { this.useSubdomain = useSubdomain; }
  public String getHeaderPrimary() { return headerPrimary; }
  public void setHeaderPrimary(String headerPrimary) { this.headerPrimary = headerPrimary; }
  public String getHeaderSecondary() { return headerSecondary; }
  public void setHeaderSecondary(String headerSecondary) { this.headerSecondary = headerSecondary; }
  public boolean isWrapDataSource() { return wrapDataSource; }
  public void setWrapDataSource(boolean wrapDataSource) { this.wrapDataSource = wrapDataSource; }
}
