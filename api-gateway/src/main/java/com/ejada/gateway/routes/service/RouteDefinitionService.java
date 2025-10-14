package com.ejada.gateway.routes.service;

import com.ejada.gateway.routes.model.RouteDefinition;
import com.ejada.gateway.routes.model.RouteDefinitionRequest;
import com.ejada.gateway.routes.model.RouteManagementView;
import com.ejada.gateway.routes.model.RouteMetadata;
import com.ejada.gateway.routes.model.RouteVariantMetricsView;
import com.ejada.gateway.routes.repository.RouteDefinitionRepository;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class RouteDefinitionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteDefinitionService.class);

  private final RouteDefinitionRepository repository;
  private final RouteDefinitionValidator validator;
  private final ApplicationEventPublisher eventPublisher;
  private final RouteVariantService variantService;

  public RouteDefinitionService(RouteDefinitionRepository repository,
      RouteDefinitionValidator validator,
      ApplicationEventPublisher eventPublisher,
      RouteVariantService variantService) {
    this.repository = repository;
    this.validator = validator;
    this.eventPublisher = eventPublisher;
    this.variantService = variantService;
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
              effective,
              variantService.metricsForRoute(route.id()));
        });
  }

  public Mono<RouteDefinition> validate(RouteDefinitionRequest request) {
    return Mono.fromCallable(() -> {
          RouteDefinition definition = request.toDomain(null, 1, Instant.now());
          return validator.validate(definition);
        })
        .subscribeOn(Schedulers.boundedElastic());
  }

  public Mono<RouteDefinition> create(RouteDefinitionRequest request, Authentication actor) {
    RouteDefinition definition = request.toDomain(null, 1, Instant.now());
    validator.validate(definition);
    return repository.create(definition, resolveActor(actor))
        .doOnSuccess(route -> publishRefreshEvent("create", route));
  }

  public Mono<RouteDefinition> update(UUID id, RouteDefinitionRequest request, Authentication actor) {
    return repository.findById(id)
        .flatMap(existing -> {
          RouteDefinition candidate = request.toDomain(existing.id(), existing.version(), existing.createdAt());
          validator.validate(candidate);
          return repository.update(candidate, resolveActor(actor))
              .doOnSuccess(route -> publishRefreshEvent("update", route));
        });
  }

  public Mono<Void> disable(UUID id, Authentication actor) {
    return repository.disable(id, resolveActor(actor))
        .doOnSuccess(route -> publishRefreshEvent("disable", route))
        .then();
  }

  public Mono<Void> delete(UUID id, Authentication actor) {
    return repository.delete(id, resolveActor(actor))
        .doOnSuccess(route -> publishRefreshEvent("delete", route))
        .then();
  }

  public Mono<RouteDefinition> toggleBlueGreen(UUID id, String slot, Authentication actor) {
    return repository.findById(id)
        .flatMap(existing -> {
          RouteMetadata.BlueGreenDeployment deployment = existing.metadata() != null
              ? copyBlueGreen(existing.metadata().getBlueGreen())
              : new RouteMetadata.BlueGreenDeployment();
          deployment.setActiveSlot(slot);
          RouteMetadata metadata = existing.metadata() != null ? existing.metadata().copy()
              : RouteMetadata.empty();
          metadata.setBlueGreen(deployment);
          RouteDefinition candidate = existing.withMetadata(metadata);
          validator.validate(candidate);
          return repository.update(candidate, resolveActor(actor))
              .doOnSuccess(route -> publishRefreshEvent("toggle-bluegreen", route));
        });
  }

  private RouteMetadata.BlueGreenDeployment copyBlueGreen(RouteMetadata.BlueGreenDeployment deployment) {
    if (deployment == null) {
      return new RouteMetadata.BlueGreenDeployment();
    }
    RouteMetadata.BlueGreenDeployment copy = new RouteMetadata.BlueGreenDeployment();
    deployment.getActiveSlot().ifPresent(copy::setActiveSlot);
    copy.setBlueUri(deployment.getBlueUri());
    copy.setGreenUri(deployment.getGreenUri());
    return copy;
  }

  public List<RouteVariantMetricsView> variantMetrics() {
    return variantService.snapshotMetrics();
  }

  private String resolveActor(Authentication authentication) {
    if (authentication == null) {
      return "system";
    }
    return authentication.getName();
  }

  private void publishRefreshEvent(String action, RouteDefinition route) {
    if (route == null) {
      return;
    }
    LOGGER.info("Publishing RefreshRoutesEvent after {} route {}", action, route.id());
    eventPublisher.publishEvent(new RefreshRoutesEvent(this));
  }
}
