package com.ejada.catalog.security;

import com.ejada.starter_security.authorization.EjadaOfficerAuthorized;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EjadaOfficerAuthorized
public @interface CatalogAuthorized {
}
