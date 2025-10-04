package com.ejada.sec.security;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that enforces a specific privilege before invoking a method.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("@permissionEvaluator.hasPermission(authentication, #permission)")
public @interface RequiresPermission {

    @AliasFor("permission")
    String value() default "";

    @AliasFor("value")
    String permission() default "";
}
