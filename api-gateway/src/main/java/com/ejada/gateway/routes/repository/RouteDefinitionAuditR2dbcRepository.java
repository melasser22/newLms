package com.ejada.gateway.routes.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RouteDefinitionAuditR2dbcRepository
    extends ReactiveCrudRepository<RouteDefinitionAuditEntity, UUID> {
}
