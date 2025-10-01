package com.ejada.starter_security.authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Grants access to EJADA platform staff (EJADA officer, tenant admin, tenant officer).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@authorizationExpressions.isPlatformStaff(authentication)")
public @interface PlatformStaffAuthorized {
}
