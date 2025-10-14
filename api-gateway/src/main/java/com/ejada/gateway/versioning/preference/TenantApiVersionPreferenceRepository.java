package com.ejada.gateway.versioning.preference;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface TenantApiVersionPreferenceRepository
    extends ReactiveCrudRepository<TenantApiVersionPreferenceEntity, Long> {

  Mono<TenantApiVersionPreferenceEntity> findFirstByTenantIdIgnoreCaseAndResource(String tenantId,
      String resource);
}
