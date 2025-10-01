package com.ejada.starter_security.authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Shared composed authorization annotation for platform services that should only be accessible
 * to EJADA platform staff (officers and tenant administrators/officers).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PlatformStaffAuthorized
public @interface PlatformServiceAuthorized {
}
