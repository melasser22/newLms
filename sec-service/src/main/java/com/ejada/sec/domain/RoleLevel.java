package com.ejada.sec.domain;

import lombok.Getter;

@Getter
public enum RoleLevel {
    PLATFORM_ADMIN(100, "EJADA_OFFICER", "Platform administrator with cross-tenant access"),
    TENANT_ADMIN(80, "TENANT_ADMIN", "Tenant owner with full tenant control"),
    TENANT_OFFICER(60, "TENANT_OFFICER", "Tenant staff with elevated permissions"),
    TENANT_USER(40, "TENANT_USER", "Regular tenant user"),
    END_USER(20, "END_USER", "Read-only consumer"),
    GUEST(0, "GUEST", "Unauthenticated access");

    private final int level;
    private final String roleCode;
    private final String description;

    RoleLevel(int level, String roleCode, String description) {
        this.level = level;
        this.roleCode = roleCode;
        this.description = description;
    }

    public boolean hasHigherOrEqualPrivilege(RoleLevel other) {
        return this.level >= other.level;
    }

    public static RoleLevel fromRoleCode(String roleCode) {
        for (RoleLevel level : values()) {
            if (level.roleCode.equals(roleCode)) {
                return level;
            }
        }
        return GUEST;
    }
}
