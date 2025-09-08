package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
  name = "privileges",
  uniqueConstraints = @UniqueConstraint(name = "ux_privileges_tenant_code", columnNames = {"tenant_id","code"}),
  indexes = @Index(name = "ix_privileges_tenant_resource_action", columnList = "tenant_id,resource,action")
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Privilege extends AuditableEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 100)
    private String code; // USER_READ, ROLE_ASSIGN ...

    @Column(nullable = false, length = 100)
    private String resource; // USER, ROLE, PRIVILEGE, TENANT...

    @Column(nullable = false, length = 50)
    private String action; // READ, CREATE, UPDATE...

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @OneToMany(mappedBy = "privilege", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePrivilege> rolePrivileges = new HashSet<>();

    @OneToMany(mappedBy = "privilege", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserPrivilege> userPrivileges = new HashSet<>();
}
