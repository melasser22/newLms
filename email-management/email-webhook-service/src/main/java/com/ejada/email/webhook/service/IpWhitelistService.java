package com.ejada.email.webhook.service;

import com.ejada.email.webhook.SendgridWebhookProperties;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
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
