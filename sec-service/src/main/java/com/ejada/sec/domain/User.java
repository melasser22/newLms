package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
  name = "users",
  uniqueConstraints = {
      @UniqueConstraint(name = "ux_users_tenant_username", columnNames = {"tenant_id", "username"}),
      @UniqueConstraint(name = "ux_users_tenant_email",    columnNames = {"tenant_id", "email"}),
      @UniqueConstraint(name = "ux_users_tenant_phone",    columnNames = {"tenant_id", "phone_number"})
  },
  indexes = {
    @Index(name = "ix_users_tenant_id", columnList = "tenant_id"),
    @Index(name = "ix_users_last_login", columnList = "last_login_at")
  }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 32)
    private String phoneNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked  = false;

    @Column(name = "last_login_at") private Instant lastLoginAt;

    @Column(name = "first_login_completed", nullable = false)
    @Builder.Default
    private boolean firstLoginCompleted = false;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "password_expires_at")
    private Instant passwordExpiresAt;

    // relations
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> roles = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RefreshToken> refreshTokens = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PasswordResetToken> resetTokens = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FederatedIdentity> federatedIdentities = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserPrivilege> userPrivileges = new HashSet<>();
}
