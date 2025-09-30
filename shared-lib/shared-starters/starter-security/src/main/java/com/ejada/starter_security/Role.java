package com.ejada.starter_security;

/**
 * Enumeration of application roles.
 */
public enum Role {
    EJADA_OFFICER("ROLE_EJADA_OFFICER"),
    TENANT_ADMIN("ROLE_TENANT_ADMIN"),
    TENANT_OFFICER("ROLE_TENANT_OFFICER");

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
