package com.ejada.gateway.dr;

import com.ejada.gateway.config.GatewayDisasterRecoveryProperties;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Tracks the currently active region and coordinates cross-region failover decisions.
 */
@Component
public class RegionFailoverService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RegionFailoverService.class);

  private final GatewayDisasterRecoveryProperties properties;
  private final AtomicReference<String> activeRegion;
  private final AtomicInteger failureCounter = new AtomicInteger();
  private volatile Instant lastFailure = Instant.EPOCH;

  public RegionFailoverService(GatewayDisasterRecoveryProperties properties) {
    this.properties = properties;
    this.activeRegion = new AtomicReference<>(properties.getPrimaryRegion());
  }

  public boolean isEnabled() {
    return properties.isEnabled();
  }

  public String currentRegion() {
    return activeRegion.get();
  }

  public void recordFailure(Throwable error) {
    if (!isEnabled()) {
      return;
    }
    Instant now = Instant.now();
    if (now.minus(properties.getFailureWindow()).isAfter(lastFailure)) {
      failureCounter.set(0);
    }
    lastFailure = now;
    int count = failureCounter.incrementAndGet();
    LOGGER.warn("Recorded failure {} for region {} due to {}", count, currentRegion(), error.toString());
    if (count >= properties.getFailureThreshold()) {
      triggerFailover("failure-threshold-exceeded");
    }
  }

  public void resetFailures() {
    if (!isEnabled()) {
      return;
    }
    failureCounter.set(0);
  }

  public synchronized String triggerFailover(String reason) {
    if (!isEnabled()) {
      return currentRegion();
    }
    Optional<String> next = nextRegion();
    if (next.isEmpty()) {
      LOGGER.warn("Failover requested but no backup regions configured. Reason: {}", reason);
      return currentRegion();
    }
    String target = next.get();
    activeRegion.set(target);
    failureCounter.set(0);
    LOGGER.warn("Failover activated to region {} (reason: {})", target, reason);
    return target;
  }

  public synchronized String restorePrimary(String reason) {
    if (!isEnabled()) {
      return currentRegion();
    }
    activeRegion.set(properties.getPrimaryRegion());
    failureCounter.set(0);
    LOGGER.info("Restored primary region {} (reason: {})", properties.getPrimaryRegion(), reason);
    return currentRegion();
  }

  private Optional<String> nextRegion() {
    List<String> backups = properties.getBackupRegions();
    if (backups == null || backups.isEmpty()) {
      return Optional.empty();
    }
    String current = currentRegion();
    return backups.stream()
        .filter(StringUtils::hasText)
        .filter(region -> !Objects.equals(region, current))
        .findFirst();
  }
}
