package com.ejada.gateway.versioning;

import com.ejada.gateway.context.GatewayRequestAttributes;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

@Service
public class VersionAnalyticsService {

  private final MeterRegistry meterRegistry;

  private final ConcurrentHashMap<String, AtomicLong> tenantVersionCounters = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, Instant> lastSeen = new ConcurrentHashMap<>();

  public VersionAnalyticsService(MeterRegistry meterRegistry) {
    this.meterRegistry = Objects.requireNonNull(meterRegistry, "meterRegistry");
  }

  public void record(ServerWebExchange exchange, VersionMappingResult result) {
    if (result == null || !StringUtils.hasText(result.getResolvedVersion())) {
      return;
    }
    String tenant = resolveTenant(exchange);
    String requested = StringUtils.hasText(result.getRequestedVersion())
        ? result.getRequestedVersion()
        : "unspecified";
    String resolved = result.getResolvedVersion();
    String source = StringUtils.hasText(result.getResolutionSource())
        ? result.getResolutionSource()
        : "unknown";

    Tags tags = Tags.of(Tag.of("tenant", tenant),
        Tag.of("requested", requested),
        Tag.of("resolved", resolved),
        Tag.of("source", source));
    meterRegistry.counter("gateway.api.version.usage", tags).increment();

    String key = tenant + '|' + resolved;
    tenantVersionCounters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
    lastSeen.put(key, Instant.now());

    if (result.isDeprecated()) {
      meterRegistry.counter("gateway.api.version.deprecated", Tags.of(
          Tag.of("tenant", tenant),
          Tag.of("requested", requested),
          Tag.of("resolved", resolved))).increment();
    }
  }

  public Map<String, Long> snapshotTenantUsage(String tenantId) {
    return tenantVersionCounters.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(tenantId + '|'))
        .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().get()));
  }

  public Instant lastSeen(String tenantId, String version) {
    return lastSeen.get(tenantId + '|' + version);
  }

  private String resolveTenant(ServerWebExchange exchange) {
    Object attribute = exchange.getAttribute(GatewayRequestAttributes.TENANT_ID);
    if (attribute instanceof String tenant && StringUtils.hasText(tenant)) {
      return tenant;
    }
    return "unknown";
  }
}
