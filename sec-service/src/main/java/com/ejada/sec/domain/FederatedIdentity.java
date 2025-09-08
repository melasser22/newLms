package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(
  name = "federated_identities",
  uniqueConstraints = @UniqueConstraint(
    name = "ux_fed_identity_provider_user",
    columnNames = {"provider","provider_user_id"}
  ),
  indexes = @Index(name = "ix_fed_identity_tenant_user", columnList = "tenant_id,user_id")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FederatedIdentity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String provider; // google, okta...

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;
}
