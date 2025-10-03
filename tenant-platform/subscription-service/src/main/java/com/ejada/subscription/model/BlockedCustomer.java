package com.ejada.subscription.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "blocked_customers",
        indexes = {
            @Index(name = "idx_blocked_customers_email", columnList = "email"),
            @Index(name = "idx_blocked_customers_domain", columnList = "domain"),
            @Index(name = "idx_blocked_customers_ext_id", columnList = "ext_customer_id")
        })
@Getter
@Setter
public class BlockedCustomer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "blocked_customer_id")
    private Long blockedCustomerId;

    @Column(name = "ext_customer_id")
    private Long extCustomerId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "domain", length = 255)
    private String domain;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "block_reason", length = 64, nullable = false)
    private String blockReason;

    @Column(name = "block_notes")
    private String blockNotes;

    @Column(name = "blocked_by", length = 128, nullable = false)
    private String blockedBy;

    @CreationTimestamp
    @Column(name = "blocked_at", nullable = false, updatable = false)
    private OffsetDateTime blockedAt;

    @Column(name = "unblocked_by", length = 128)
    private String unblockedBy;

    @UpdateTimestamp
    @Column(name = "unblocked_at")
    private OffsetDateTime unblockedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = Boolean.TRUE;
}
