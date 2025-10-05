package com.ejada.gateway.routes.service;

import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteDefinitionRequest;
import com.ejada.gateway.routes.model.RouteManagementView;
import com.ejada.gateway.routes.repository.RouteDefinitionRepository;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class RouteDefinitionService {

  private final RouteDefinitionRepository repository;
  private final RouteDefinitionValidator validator;

  public RouteDefinitionService(RouteDefinitionRepository repository,
      RouteDefinitionValidator validator) {
    this.repository = repository;
    this.validator = validator;
  }

  public Flux<RouteDefinition> findAll() {
    return repository.findAll();
  }

  public Flux<RouteManagementView> fetchManagementView() {
    return repository.findAll()
        .map(route -> {
          URI effective = route.metadata().resolveEffectiveUri(route.serviceUri()).orElse(route.serviceUri());
          return new RouteManagementView(
              route.id(),
              route.pathPattern(),
              route.serviceUri(),
              route.enabled(),
              route.version(),
              route.updatedAt(),
              route.metadata().getBlueGreen(),
              route.metadata().getTrafficSplits(),
              effective);
        });
  }

  public Mono<RouteDefinition> create(RouteDefinitionRequest request, Authentication actor) {
    RouteDefinition definition = request.toDomain(null, 1, Instant.now());
    validator.validate(definition);
    return repository.create(definition, resolveActor(actor));
  }

  public Mono<RouteDefinition> update(UUID id, RouteDefinitionRequest request, Authentication actor) {
    return repository.findById(id)
        .flatMap(existing -> {
          RouteDefinition candidate = request.toDomain(existing.id(), existing.version(), existing.createdAt());
          validator.validate(candidate);
          return repository.update(candidate, resolveActor(actor));
        });
  }

  public Mono<Void> disable(UUID id, Authentication actor) {
    return repository.disable(id, resolveActor(actor)).then();
  }

  private String resolveActor(Authentication authentication) {
    if (authentication == null) {
      return "system";
    }
    return authentication.getName();
  }
}
