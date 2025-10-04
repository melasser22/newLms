package com.ejada.sec.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Defines the hierarchical levels for roles in the system.
 *
 * <p>The level determines a role's position in the hierarchy, where higher
 * numeric values represent more privileged roles. This hierarchy is used to
 * enforce management rules such as "users can only manage users with lower roles."
 *
 * <p><b>Hierarchy Rules:</b>
 * <ul>
 *   <li>A user can only manage users with strictly lower role levels</li>
 *   <li>A user can only assign roles with strictly lower levels</li>
 *   <li>Platform admins (PLATFORM_ADMIN) are exempt from these rules</li>
 *   <li>Same-tenant requirement applies for non-platform roles</li>
 * </ul>
 *
 * <p><b>Example Scenarios:</b>
 * <pre>
 * TENANT_ADMIN (80) can manage:
 *   ✓ TENANT_OFFICER (60)
 *   ✓ TENANT_USER (40)
 *   ✓ END_USER (20)
 *   ✗ PLATFORM_ADMIN (100) - higher level
 *   ✗ Other TENANT_ADMIN (80) - same level
 *
 * TENANT_OFFICER (60) can manage:
 *   ✓ TENANT_USER (40)
 *   ✓ END_USER (20)
 *   ✗ TENANT_ADMIN (80) - higher level
 *   ✗ Other TENANT_OFFICER (60) - same level
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum RoleLevel {

    /**
     * Platform administrator with cross-tenant access and system-wide privileges.
     * Can manage all tenants, users, and system configuration.
     */
    PLATFORM_ADMIN(100, "EJADA_OFFICER", "Platform Administrator",
        "Full platform access across all tenants with system configuration privileges"),

    /**
     * Tenant owner with full control over their tenant.
     * Can manage billing, subscription, and all users within the tenant.
     */
    TENANT_ADMIN(80, "TENANT_ADMIN", "Tenant Administrator",
        "Full control over tenant including billing, users, and configuration"),

    /**
     * Tenant manager with elevated privileges within the tenant.
     * Can manage regular users but not other admins or officers.
     */
    TENANT_OFFICER(60, "TENANT_OFFICER", "Tenant Officer",
        "Manage regular users and content within tenant, limited configuration access"),

    /**
     * Regular tenant user with standard application access.
     * Can perform CRUD operations on their own data.
     */
    TENANT_USER(40, "TENANT_USER", "Regular User",
        "Standard application access with CRUD on own data"),

    /**
     * Read-only consumer with minimal permissions.
     * Typically used for public-facing or customer portal access.
     */
    END_USER(20, "END_USER", "End User",
        "Read-only consumer access to published content"),

    /**
     * Unauthenticated access level.
     * Only public endpoints are accessible.
     */
    GUEST(0, "GUEST", "Guest",
        "Unauthenticated access to public endpoints only");

    /**
     * Numeric level determining hierarchy position (0-100).
     * Higher values = more privileged roles.
     */
    private final int level;

    /**
     * Role code matching the database role.code column.
     * Used for lookups and assignments.
     */
    private final String roleCode;

    /**
     * Human-readable role name for UI display.
     */
    private final String displayName;

    /**
     * Detailed description of role capabilities.
     */
    private final String description;

    /**
     * Checks if this role has higher or equal privilege level than another role.
     *
     * <p><b>Usage:</b>
     * <pre>{@code
     * RoleLevel admin = RoleLevel.TENANT_ADMIN;
     * RoleLevel user = RoleLevel.TENANT_USER;
     *
     * admin.hasHigherOrEqualPrivilege(user);  // true (80 >= 40)
     * user.hasHigherOrEqualPrivilege(admin);  // false (40 >= 80)
     * admin.hasHigherOrEqualPrivilege(admin); // true (80 >= 80)
     * }</pre>
     *
     * @param other the role level to compare against
     * @return true if this role's level >= other's level
     */
    public boolean hasHigherOrEqualPrivilege(RoleLevel other) {
        return this.level >= other.level;
    }

    /**
     * Checks if this role has strictly higher privilege level than another role.
     *
     * <p>Use this for management rules: "can only manage users with lower roles"
     *
     * @param other the role level to compare against
     * @return true if this role's level > other's level
     */
    public boolean hasHigherPrivilege(RoleLevel other) {
        return this.level > other.level;
    }

    /**
     * Checks if this is a platform-level role (cross-tenant access).
     *
     * @return true if this is PLATFORM_ADMIN
     */
    public boolean isPlatformRole() {
        return this == PLATFORM_ADMIN;
    }

    /**
     * Checks if this is a tenant-level administrative role.
     *
     * @return true if this is TENANT_ADMIN or TENANT_OFFICER
     */
    public boolean isTenantAdminRole() {
        return this == TENANT_ADMIN || this == TENANT_OFFICER;
    }

    /**
     * Gets role level by role code.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * RoleLevel level = RoleLevel.fromRoleCode("TENANT_ADMIN");
     * // Returns: TENANT_ADMIN (level 80)
     *
     * RoleLevel unknown = RoleLevel.fromRoleCode("INVALID");
     * // Returns: GUEST (level 0) - default fallback
     * }</pre>
     *
     * @param roleCode the role code to look up
     * @return matching RoleLevel or GUEST if not found
     */
    public static RoleLevel fromRoleCode(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return GUEST;
        }

        return Arrays.stream(values())
            .filter(level -> level.roleCode.equalsIgnoreCase(roleCode))
            .findFirst()
            .orElse(GUEST);
    }

    /**
     * Gets all role levels that are manageable by this role level.
     *
     * <p>Returns all levels with strictly lower numeric value.
     *
     * <p><b>Example:</b>
     * <pre>{@code
     * RoleLevel.TENANT_ADMIN.getManageableRoles();
     * // Returns: [TENANT_OFFICER, TENANT_USER, END_USER, GUEST]
     *
     * RoleLevel.TENANT_USER.getManageableRoles();
     * // Returns: [END_USER, GUEST]
     * }</pre>
     *
     * @return list of role levels this role can manage
     */
    public List<RoleLevel> getManageableRoles() {
        return Arrays.stream(values())
            .filter(level -> this.level > level.level)
            .sorted(Comparator.comparingInt(RoleLevel::getLevel).reversed())
            .collect(Collectors.toList());
    }

    /**
     * Gets all role levels that can be assigned by this role level.
     * Same as getManageableRoles() - you can only assign roles you can manage.
     *
     * @return list of role levels this role can assign
     */
    public List<RoleLevel> getAssignableRoles() {
        return getManageableRoles();
    }

    /**
     * Validates if this role can manage a target role.
     *
     * @param targetRole the role to check management permission for
     * @return true if this role can manage the target role
     */
    public boolean canManage(RoleLevel targetRole) {
        return this.level > targetRole.level;
    }

    /**
     * Validates if this role can assign a specific role.
     *
     * @param roleToAssign the role to check assignment permission for
     * @return true if this role can assign the specified role
     */
    public boolean canAssign(RoleLevel roleToAssign) {
        return this.level > roleToAssign.level;
    }

    /**
     * Gets the highest role level from a collection of role codes.
     *
     * @param roleCodes collection of role codes
     * @return highest RoleLevel or GUEST if collection is empty
     */
    public static RoleLevel getHighestLevel(List<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return GUEST;
        }

        return roleCodes.stream()
            .map(RoleLevel::fromRoleCode)
            .max(Comparator.comparingInt(RoleLevel::getLevel))
            .orElse(GUEST);
    }

    @Override
    public String toString() {
        return String.format("%s (Level %d)", displayName, level);
    }
}
