package com.ejada.gateway.routes.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record RouteDefinitionRequest(
    @NotBlank String pathPattern,
    @NotBlank String serviceUri,
    List<@Valid RouteComponent> predicates,
    List<@Valid RouteComponent> filters,
    RouteMetadata metadata,
    boolean enabled) {

  public RouteDefinition toDomain(UUID id, int version, Instant createdAt) {
    RouteMetadata resolvedMetadata = (metadata == null) ? RouteMetadata.empty() : metadata;
    URI uri = StringUtils.hasText(serviceUri) ? URI.create(serviceUri.trim()) : null;
    return new RouteDefinition(
        id,
        pathPattern,
        uri,
        predicates,
        filters,
        resolvedMetadata,
        enabled,
        version,
        createdAt,
        Instant.now());
  }
}
