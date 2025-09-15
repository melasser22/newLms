package com.ejada.subscription.repository;

import com.ejada.subscription.model.IdempotentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, UUID>, JpaSpecificationExecutor<IdempotentRequest> {

    boolean existsByIdempotencyKey(UUID idempotencyKey);
}
