package com.ejada.gateway.admin.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated view returned from {@code /api/v1/admin/overview}.
 */
public record AdminOverview(
    Instant generatedAt,
    int totalServices,
    long upServices,
    long downServices,
    long degradedServices,
    List<AdminServiceSnapshot> services) {

  public static AdminOverview fromSnapshots(List<AdminServiceSnapshot> snapshots) {
    if (snapshots == null || snapshots.isEmpty()) {
      return empty();
    }
    long up = snapshots.stream().filter(snapshot -> snapshot.state() == AdminServiceState.UP).count();
    long down = snapshots.stream().filter(snapshot -> snapshot.state() == AdminServiceState.DOWN).count();
    long degraded = snapshots.stream()
        .filter(snapshot -> snapshot.state() == AdminServiceState.DEGRADED)
        .count();
    return new AdminOverview(
        Instant.now(),
        snapshots.size(),
        up,
        down,
        degraded,
        List.copyOf(snapshots));
  }

  public static AdminOverview empty() {
    return new AdminOverview(Instant.now(), 0, 0, 0, 0, Collections.emptyList());
  }
}
