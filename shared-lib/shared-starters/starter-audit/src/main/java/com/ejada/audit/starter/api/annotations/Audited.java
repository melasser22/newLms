package com.ejada.audit.starter.api.annotations;

import com.ejada.audit.starter.api.AuditAction;
import com.ejada.audit.starter.api.AuditOutcome;
import com.ejada.audit.starter.api.Sensitivity;
import com.ejada.audit.starter.api.DataClass;
import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {
  AuditAction action() default AuditAction.OTHER;
  String entity() default "";
  String entityIdExpr() default ""; // SpEL from args/return
  Sensitivity sensitivity() default Sensitivity.INTERNAL;
  DataClass dataClass() default DataClass.NONE;
  AuditOutcome outcome() default AuditOutcome.SUCCESS;
  String message() default "";
  boolean captureDiff() default false;
}
