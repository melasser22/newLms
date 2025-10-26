package com.ejada.gateway.routes.repository;

import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RouteCallAuditR2dbcRepository
    extends ReactiveCrudRepository<RouteCallAuditEntity, UUID> {}

