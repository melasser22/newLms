package com.ejada.config.refresh;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple tracker that maintains a monotonically increasing configuration
 * version. Each successful refresh bumps the counter allowing operators to
 * correlate reloads with audit events.
 */
public class ConfigVersionTracker {

  private final AtomicLong version = new AtomicLong(1L);

  public long incrementAndGet() {
    return version.incrementAndGet();
  }

  public long getCurrentVersion() {
    return version.get();
  }
}
