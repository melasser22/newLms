package com.ejada.tenant.security;

import com.ejada.starter_security.authorization.TenantAccessAuthorized;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@TenantAccessAuthorized
public @interface TenantAuthorized {
}
