package com.ejada.gateway.routes.repository;

import com.ejada.gateway.routes.model.RouteComponent;
import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RouteDefinitionMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteDefinitionMapper.class);

  private static final TypeReference<List<RouteComponent>> COMPONENT_LIST =
      new TypeReference<>() {
      };

  private final ObjectMapper objectMapper;

  public RouteDefinitionMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public RouteDefinition toDomain(RouteDefinitionEntity entity) {
    if (entity == null) {
      return null;
    }
    try {
      List<RouteComponent> predicates = readComponents(entity.getPredicates());
      List<RouteComponent> filters = readComponents(entity.getFilters());
      RouteMetadata metadata = readMetadata(entity.getMetadata());
      URI uri = StringUtils.hasText(entity.getServiceUri()) ? URI.create(entity.getServiceUri()) : null;
      return new RouteDefinition(
          entity.getId(),
          entity.getPathPattern(),
          uri,
          predicates,
          filters,
          metadata,
          entity.isEnabled(),
          entity.getVersion(),
          entity.getCreatedAt(),
          entity.getUpdatedAt());
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to map route entity " + entity.getId(), ex);
    }
  }

  public RouteDefinitionEntity toEntity(RouteDefinition definition) {
    RouteDefinitionEntity entity = new RouteDefinitionEntity();
    entity.setId(definition.id());
    entity.setPathPattern(definition.pathPattern());
    entity.setServiceUri(definition.serviceUri() != null ? definition.serviceUri().toString() : null);
    entity.setPredicates(writeComponents(definition.predicates()));
    entity.setFilters(writeComponents(definition.filters()));
    entity.setMetadata(writeMetadata(definition.metadata()));
    entity.setEnabled(definition.enabled());
    entity.setVersion(definition.version());
    entity.setCreatedAt(definition.createdAt());
    entity.setUpdatedAt(definition.updatedAt());
    return entity;
  }

  public List<RouteDefinition> decodeList(String payload) {
    if (!StringUtils.hasText(payload)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(payload, new TypeReference<List<RouteDefinition>>() {
      });
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to decode cached route definitions", ex);
    }
  }

  public String encodeList(List<RouteDefinition> routes) {
    try {
      return objectMapper.writeValueAsString(routes);
    } catch (Exception ex) {
      throw new UncheckedIOException("Unable to serialise route definitions", new java.io.IOException(ex));
    }
  }

  private RouteMetadata readMetadata(String payload) {
    if (!StringUtils.hasText(payload)) {
      return RouteMetadata.empty();
    }
    try {
      return objectMapper.readValue(payload, RouteMetadata.class);
    } catch (Exception ex) {
      LOGGER.warn("Failed to parse route metadata: {}", payload, ex);
      return RouteMetadata.empty();
    }
  }

  private List<RouteComponent> readComponents(String payload) {
    if (!StringUtils.hasText(payload)) {
      return List.of();
    }
    try {
      return objectMapper.readValue(payload, COMPONENT_LIST);
    } catch (Exception ex) {
      LOGGER.warn("Failed to parse route components: {}", payload, ex);
      return List.of();
    }
  }

  private String writeComponents(List<RouteComponent> components) {
    try {
      return objectMapper.writeValueAsString((components == null) ? Collections.emptyList() : components);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encode route components", ex);
    }
  }

  private String writeMetadata(RouteMetadata metadata) {
    try {
      return objectMapper.writeValueAsString(metadata == null ? RouteMetadata.empty() : metadata);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encode route metadata", ex);
    }
  }

  public RouteDefinitionAuditEntity toAuditEntity(RouteDefinition route, String changeType,
      String changedBy) {
    RouteDefinitionAuditEntity audit = new RouteDefinitionAuditEntity();
    audit.setAuditId(UUID.randomUUID());
    audit.setRouteId(route.id());
    audit.setChangeType(changeType);
    audit.setChangedBy(changedBy);
    audit.setChangedAt(Instant.now());
    audit.setVersion(route.version());
    audit.setPayload(writeAuditPayload(route));
    return audit;
  }

  private String writeAuditPayload(RouteDefinition route) {
    try {
      return objectMapper.writeValueAsString(route);
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to encode audit payload", ex);
    }
  }
}
