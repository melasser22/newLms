package com.ejada.gateway.security.mtls;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface PartnerCertificateRepository
    extends ReactiveCrudRepository<PartnerCertificateEntity, Long> {

  Flux<PartnerCertificateEntity> findByTenantIdAndRevokedFalse(String tenantId);
}
