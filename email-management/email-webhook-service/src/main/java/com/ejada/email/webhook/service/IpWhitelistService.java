package com.ejada.email.webhook.service;

import com.ejada.email.webhook.SendgridWebhookProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
@SuppressFBWarnings(
    value = "EI_EXPOSE_REP2",
    justification = "Properties are managed by Spring and not mutated externally")
public class IpWhitelistService {

  private final SendgridWebhookProperties properties;

  public IpWhitelistService(SendgridWebhookProperties properties) {
    this.properties = properties;
  }

  public boolean isAllowed(String ip) {
    List<String> allowed = properties.getAllowedIps();
    if (allowed == null || allowed.isEmpty()) {
      return true;
    }
    return allowed.contains(ip);
  }
}
