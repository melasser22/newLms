package com.ejada.starter_data.tenant;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited  // class-level annotation is inherited by subclasses (methods can override)
public @interface TenantOnly {}
