package com.ejada.setup.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@roleChecker.hasRole(authentication, T(com.ejada.starter_security.Role).EJADA_OFFICER, T(com.ejada.starter_security.Role).TENANT_ADMIN, T(com.ejada.starter_security.Role).TENANT_OFFICER)")
public @interface SetupAuthorized {
}
