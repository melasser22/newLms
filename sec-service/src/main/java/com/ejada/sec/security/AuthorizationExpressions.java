package com.ejada.sec.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component("authorizationExpressions")
public class AuthorizationExpressions {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationExpressions.class);
    private static final String REQUIRED_ROLE = "ROLE_EJADA_OFFICER";

    public boolean isEjadaOfficer(Authentication authentication) {
        if (authentication == null) {
            log.debug("Denied EJADA officer check because authentication is null");
            return false;
        }

        if (!authentication.isAuthenticated()) {
            log.debug("Denied EJADA officer check because authentication {} is not authenticated", authentication);
            return false;
        }

        boolean hasRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(REQUIRED_ROLE::equals);

        if (log.isDebugEnabled()) {
            String principal = authentication.getName();
            if (principal == null) {
                principal = "unknown";
            }
            log.debug("Evaluated EJADA officer access for principal '{}' with authorities {} => {}",
                    principal, authentication.getAuthorities(), hasRole);
        }

        return hasRole;
    }
}
