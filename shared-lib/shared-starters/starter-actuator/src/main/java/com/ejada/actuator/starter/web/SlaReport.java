package com.ejada.actuator.starter.web;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO returned by the /sla/report endpoint.
 */
public record SlaReport(
    String service,
    List<String> profiles,
    String status,
    OffsetDateTime generatedAt,
    Duration uptime,
    OffsetDateTime startedAt,
    Build build,
    Runtime runtime,
    Metadata metadata,
    Map<String, Object> info,
    Map<String, Object> health
) {

  public record Build(String version, java.time.Instant time, String commit, String branch) {}

  public record Runtime(
      long pid,
      String host,
      String address,
      String region,
      String zone,
      String pod,
      String node
  ) {}

  public record Metadata(String owner, String contact, String description) {}
}
