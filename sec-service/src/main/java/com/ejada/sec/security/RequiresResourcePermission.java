package com.ejada.sec.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that validates resource/action authorization before method execution.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@permissionEvaluator.hasResourcePermission(authentication, #resource, #action)")
public @interface RequiresResourcePermission {

    String resource();

    String action();
}
