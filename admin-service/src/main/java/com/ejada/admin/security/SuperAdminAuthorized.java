package com.ejada.admin.security;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@roleChecker.hasRole(authentication, T(com.ejada.starter_security.Role).EJADA_OFFICER)")
public @interface SuperAdminAuthorized {}
