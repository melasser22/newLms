package com.ejada.gateway.routes.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RouteManagementView(
    UUID id,
    String pathPattern,
    URI serviceUri,
    boolean enabled,
    int version,
    Instant updatedAt,
    RouteMetadata.BlueGreenDeployment blueGreen,
    List<RouteMetadata.TrafficSplit> trafficSplits,
    URI effectiveUri) {
}
