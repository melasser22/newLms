package com.ejada.sec.security;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize(
    "@roleChecker.hasRole(authentication, "
        + "T(com.ejada.starter_security.Role).EJADA_SUPERADMIN, "
        + "T(com.ejada.starter_security.Role).EJADA_OFFICER, "
        + "T(com.ejada.starter_security.Role).TENANT_ADMIN, "
        + "T(com.ejada.starter_security.Role).TENANT_OFFICER)")
public @interface SecAuthorized {
}
