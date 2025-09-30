package com.ejada.starter_security.authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Grants access exclusively to EJADA officers.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@roleChecker.hasRole(authentication, T(com.ejada.starter_security.Role).EJADA_OFFICER)")
public @interface EjadaOfficerAuthorized {
}
