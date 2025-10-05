package com.ejada.gateway.routes.model;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain representation of a database-backed route definition.
 */
public record RouteDefinition(
    UUID id,
    String pathPattern,
    URI serviceUri,
    List<RouteComponent> predicates,
    List<RouteComponent> filters,
    RouteMetadata metadata,
    boolean enabled,
    int version,
    Instant createdAt,
    Instant updatedAt) {

  public RouteDefinition {
    predicates = sanitise(predicates);
    filters = sanitise(filters);
    metadata = (metadata == null) ? RouteMetadata.empty() : metadata;
  }

  private static List<RouteComponent> sanitise(List<RouteComponent> components) {
    if (components == null || components.isEmpty()) {
      return List.of();
    }
    return List.copyOf(new ArrayList<>(components));
  }

  public RouteDefinition withId(UUID newId) {
    return new RouteDefinition(newId, pathPattern, serviceUri, predicates, filters, metadata,
        enabled, version, createdAt, updatedAt);
  }

  public RouteDefinition withVersion(int newVersion, Instant updated) {
    return new RouteDefinition(id, pathPattern, serviceUri, predicates, filters, metadata,
        enabled, newVersion, createdAt, updated);
  }

  public RouteDefinition withState(boolean newEnabled, Instant updated) {
    return new RouteDefinition(id, pathPattern, serviceUri, predicates, filters, metadata,
        newEnabled, version, createdAt, updated);
  }

  public RouteDefinition withMetadata(RouteMetadata newMetadata) {
    return new RouteDefinition(id, pathPattern, serviceUri, predicates, filters,
        (newMetadata == null) ? RouteMetadata.empty() : newMetadata,
        enabled, version, createdAt, updatedAt);
  }

  public RouteDefinition updateFrom(RouteDefinition candidate) {
    return new RouteDefinition(
        id,
        candidate.pathPattern,
        candidate.serviceUri,
        candidate.predicates,
        candidate.filters,
        candidate.metadata,
        candidate.enabled,
        candidate.version,
        createdAt,
        candidate.updatedAt);
  }

  public RouteDefinition withTimestamps(Instant created, Instant updated) {
    return new RouteDefinition(id, pathPattern, serviceUri, predicates, filters, metadata,
        enabled, version, created, updated);
  }

  public RouteDefinition withServiceUri(URI uri) {
    return new RouteDefinition(id, pathPattern, uri, predicates, filters, metadata,
        enabled, version, createdAt, updatedAt);
  }

  public RouteDefinition requireId() {
    if (id == null) {
      throw new IllegalStateException("Route identifier has not been initialised");
    }
    return this;
  }

  public boolean hasTrafficSplits() {
    return metadata != null && !metadata.getTrafficSplits().isEmpty();
  }

  public RouteDefinition ensureServiceUri() {
    if (serviceUri != null) {
      return this;
    }
    URI resolved = metadata.resolveEffectiveUri(null).orElse(null);
    if (resolved == null) {
      throw new IllegalStateException("Route definition " + id + " does not specify a service URI");
    }
    return withServiceUri(resolved);
  }

  @Override
  public String toString() {
    return "RouteDefinition{" +
        "id=" + id +
        ", pathPattern='" + pathPattern + '\'' +
        ", serviceUri=" + serviceUri +
        ", enabled=" + enabled +
        ", version=" + version +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RouteDefinition that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
