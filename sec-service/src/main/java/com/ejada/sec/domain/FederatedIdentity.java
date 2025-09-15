package com.ejada.sec.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
  name = "federated_identities",
  uniqueConstraints = @UniqueConstraint(
    name = "ux_fed_identity_provider_user",
    columnNames = {"provider", "provider_user_id"}
  ),
  indexes = @Index(name = "ix_fed_identity_tenant_user", columnList = "tenant_id,user_id")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FederatedIdentity {

    private static final int PROVIDER_LENGTH = 64;
    private static final int PROVIDER_USER_ID_LENGTH = 255;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = PROVIDER_LENGTH)
    private String provider; // google, okta...

    @Column(name = "provider_user_id", nullable = false, length = PROVIDER_USER_ID_LENGTH)
    private String providerUserId;
}
