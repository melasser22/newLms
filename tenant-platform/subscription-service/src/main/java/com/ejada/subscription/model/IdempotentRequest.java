package com.ejada.subscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "idempotent_request")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IdempotentRequest {

    @Id
    @Column(name = "idempotency_key", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private UUID idempotencyKey; // rqUID

    @Column(name = "endpoint", length = 128, nullable = false)
    private String endpoint;

    @Column(name = "request_hash", length = 128, nullable = false)
    private String requestHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
