package com.ejada.common.constants;

/**
 * Shared JWT claim names to keep producers and consumers aligned.
 */
public final class JwtClaims {

    public static final String TENANT = "tenant";
    public static final String ROLES = "roles";

    private JwtClaims() {
        // utility class
    }
}
