package com.shared.starter_security;

/**
 * Enumeration of application roles.
 */
public enum Role {
    EJADA_OFFICER("ROLE_EJADA_OFFICER"),
    TENANT_ADMIN("ROLE_TenantAdmin"),
    TENANT_OFFICER("ROLE_TenantOfficer");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    /**
     * @return full authority string including the ROLE_ prefix
     */
    public String getAuthority() {
        return authority;
    }
}
