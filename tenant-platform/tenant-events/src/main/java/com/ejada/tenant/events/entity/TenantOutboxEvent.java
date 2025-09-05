package com.ejada.tenant.events.entity;

import com.ejada.starter_data.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity representing a pending tenant event stored in the outbox.
 */
@Getter
@Setter
@Entity
@Table(name = "tenant_outbox",schema = "tenant_core")
public class TenantOutboxEvent extends TenantBaseEntity {

    @Column(nullable = false)
    private String type;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(nullable = false)
    private int attempts = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.NEW;

    public enum Status { NEW, SENT }
}
