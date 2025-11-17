package com.ejada.push.usage.service;

import com.ejada.push.usage.model.UsageRecord;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class UsageService {

  private final Map<String, Map<String, UsageRecord>> usageByTenant = new ConcurrentHashMap<>();

  public void recordEvent(String tenantId, UsageEvent event) {
    usageByTenant.putIfAbsent(tenantId, new ConcurrentHashMap<>());
    Map<String, UsageRecord> tenantUsage = usageByTenant.get(tenantId);
    UsageRecord record = tenantUsage.computeIfAbsent(event.templateKey(), key -> new UsageRecord());
    switch (event.status().toUpperCase()) {
      case "SENT" -> record.incrementSent();
      case "DELIVERED" -> record.incrementDelivered();
      case "OPENED" -> record.incrementOpened();
      default -> {
        // ignore unknown
      }
    }
  }

  public UsageSummary summarize(String tenantId) {
    Map<String, UsageRecord> tenantUsage = usageByTenant.get(tenantId);
    if (tenantUsage == null || tenantUsage.isEmpty()) {
      return new UsageSummary("all", 0, 0, 0);
    }
    long sent = 0;
    long delivered = 0;
    long opened = 0;
    for (UsageRecord record : tenantUsage.values()) {
      sent += record.getSent();
      delivered += record.getDelivered();
      opened += record.getOpened();
    }
    return new UsageSummary("all", sent, delivered, opened);
  }
}
