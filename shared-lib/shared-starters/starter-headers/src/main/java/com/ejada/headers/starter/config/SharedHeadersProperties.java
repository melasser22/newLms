
package com.ejada.headers.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.ejada.common.constants.HeaderNames;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "shared.headers")
public class SharedHeadersProperties {
  private boolean enabled = true;

  private final Names correlation = new Names(HeaderNames.CORRELATION_ID, true, true);
  private final Names request = new Names(HeaderNames.REQUEST_ID, true, true);
  private final Names tenant = new Names(HeaderNames.X_TENANT_ID, false, false);
  private final Names user = new Names(HeaderNames.USER_ID, false, false);

  private final Mdc mdc = new Mdc();
  private final Security security = new Security();
  private final Forwarded forwarded = new Forwarded();
  private final Propagation propagation = new Propagation();

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }

  public Names getCorrelation() { return correlation; }
  public Names getRequest() { return request; }
  public Names getTenant() { return tenant; }
  public Names getUser() { return user; }
  public Mdc getMdc() { return mdc; }
  public Security getSecurity() { return security; }
  public Forwarded getForwarded() { return forwarded; }
  public Propagation getPropagation() { return propagation; }

  public static class Names {
    private String header;
    private boolean autoGenerate = false;
    private boolean mandatory = false;

    public Names() {}
    public Names(String header) { this.header = header; }
    public Names(String header, boolean autoGenerate) {
      this.header = header;
      this.autoGenerate = autoGenerate;
    }
    public Names(String header, boolean autoGenerate, boolean mandatory) {
      this.header = header;
      this.autoGenerate = autoGenerate;
      this.mandatory = mandatory;
    }

    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }

    public boolean isAutoGenerate() { return autoGenerate; }
    public void setAutoGenerate(boolean autoGenerate) { this.autoGenerate = autoGenerate; }

    public boolean isMandatory() { return mandatory; }
    public void setMandatory(boolean mandatory) { this.mandatory = mandatory; }
  }

  public static class Mdc {
    private boolean enabled = true;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
  }

  public static class Security {
    private boolean enabled = true;
    private final Hsts hsts = new Hsts();
    private String frameOptions = "SAMEORIGIN";
    private String contentTypeOptions = "nosniff";
    private String referrerPolicy = "no-referrer";
    private String permissionsPolicy = "";
    private String csp = "";
    private String coop = "same-origin";
    private String coep = "";
    private String xDownloadOptions = "noopen";
    private String xssProtection = "0";
    private List<String> excludePaths = new ArrayList<>();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Hsts getHsts() { return hsts; }
    public String getFrameOptions() { return frameOptions; }
    public void setFrameOptions(String frameOptions) { this.frameOptions = frameOptions; }
    public String getContentTypeOptions() { return contentTypeOptions; }
    public void setContentTypeOptions(String contentTypeOptions) { this.contentTypeOptions = contentTypeOptions; }
    public String getReferrerPolicy() { return referrerPolicy; }
    public void setReferrerPolicy(String referrerPolicy) { this.referrerPolicy = referrerPolicy; }
    public String getPermissionsPolicy() { return permissionsPolicy; }
    public void setPermissionsPolicy(String permissionsPolicy) { this.permissionsPolicy = permissionsPolicy; }
    public String getCsp() { return csp; }
    public void setCsp(String csp) { this.csp = csp; }
    public String getCoop() { return coop; }
    public void setCoop(String coop) { this.coop = coop; }
    public String getCoep() { return coep; }
    public void setCoep(String coep) { this.coep = coep; }
    public String getxDownloadOptions() { return xDownloadOptions; }
    public void setxDownloadOptions(String xDownloadOptions) { this.xDownloadOptions = xDownloadOptions; }
    public String getXssProtection() { return xssProtection; }
    public void setXssProtection(String xssProtection) { this.xssProtection = xssProtection; }
    public List<String> getExcludePaths() { return excludePaths; }
  }

  public static class Hsts {
    private boolean enabled = true;
    private long maxAge = 31536000;
    private boolean includeSubdomains = true;
    private boolean preload = false;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public long getMaxAge() { return maxAge; }
    public void setMaxAge(long maxAge) { this.maxAge = maxAge; }
    public boolean isIncludeSubdomains() { return includeSubdomains; }
    public void setIncludeSubdomains(boolean includeSubdomains) { this.includeSubdomains = includeSubdomains; }
    public boolean isPreload() { return preload; }
    public void setPreload(boolean preload) { this.preload = preload; }
  }

  public static class Forwarded {
    private boolean enabled = true;
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
  }

  public static class Propagation {
    private boolean enabled = true;
    private List<String> include = List.of(HeaderNames.CORRELATION_ID,HeaderNames.REQUEST_ID,HeaderNames.X_TENANT_ID,HeaderNames.USER_ID);
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getInclude() { return include; }
    public void setInclude(List<String> include) { this.include = include; }
  }
}
