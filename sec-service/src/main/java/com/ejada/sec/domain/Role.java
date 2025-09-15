package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
  name = "roles",
    uniqueConstraints = @UniqueConstraint(name = "ux_roles_tenant_code", columnNames = {"tenant_id", "code"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Role extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 64)
    private String code; // ADMIN, USER...

    @Column(nullable = false, length = 128)
    private String name;

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePrivilege> rolePrivileges = new HashSet<>();
}
