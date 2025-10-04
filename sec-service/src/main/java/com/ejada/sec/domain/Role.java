package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Role entity representing a hierarchical role within a tenant.
 * Roles are organized by level (0-100) where higher levels have more privileges.
 */
@Entity
@Table(
    name = "roles",
    uniqueConstraints = @UniqueConstraint(
        name = "ux_roles_tenant_code",
        columnNames = {"tenant_id", "code"}
    ),
    indexes = {
        @Index(name = "ix_roles_tenant_id", columnList = "tenant_id"),
        @Index(name = "ix_roles_level", columnList = "role_level"),
        @Index(name = "ix_roles_system", columnList = "is_system_role")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tenant this role belongs to.
     * Platform-level roles use a special "platform" tenant ID.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /**
     * Unique code identifying this role (e.g., "TENANT_ADMIN", "USER").
     * Must be unique within a tenant.
     */
    @Column(nullable = false, length = 64)
    private String code;

    /**
     * Human-readable role name for display.
     */
    @Column(nullable = false, length = 128)
    private String name;

    /**
     * Hierarchical level determining role privileges and management capabilities.
     * Higher values = more privileged.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role_level", nullable = false, length = 50)
    @Builder.Default
    private RoleLevel level = RoleLevel.TENANT_USER;

    /**
     * Indicates if this is a system-defined role that cannot be deleted.
     * System roles are created during tenant provisioning.
     */
    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private Boolean systemRole = false;

    /**
     * Optional description of role capabilities.
     */
    @Column(length = 500)
    private String description;

    /**
     * Users assigned to this role.
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    /**
     * Privileges granted to this role.
     */
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RolePrivilege> rolePrivileges = new HashSet<>();

    /**
     * Checks if this role can manage another role based on hierarchy level.
     *
     * @param targetRole the role to compare against
     * @return {@code true} if this role is strictly higher in the hierarchy
     */
    public boolean canManage(Role targetRole) {
        return this.level.hasHigherPrivilege(targetRole.level);
    }

    /**
     * Checks if this is a platform-level role.
     *
     * @return {@code true} if this role represents platform administration
     */
    public boolean isPlatformRole() {
        return this.level.isPlatformRole();
    }

    /**
     * Checks if this is a tenant administrative role.
     *
     * @return {@code true} if this role is tenant admin or officer
     */
    public boolean isTenantAdminRole() {
        return this.level.isTenantAdminRole();
    }

    /**
     * Prevents deletion of system roles.
     */
    @PreRemove
    public void preventSystemRoleDeletion() {
        if (Boolean.TRUE.equals(systemRole)) {
            throw new IllegalStateException(
                "Cannot delete system role: " + code + ". System roles are protected.");
        }
    }
}
