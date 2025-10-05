package com.ejada.gateway.routes.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface RouteDefinitionR2dbcRepository
    extends ReactiveCrudRepository<RouteDefinitionEntity, UUID> {

  Flux<RouteDefinitionEntity> findAllByEnabledTrue();
}
