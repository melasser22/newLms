package com.shared.audit.starter.core.aop;

import com.shared.audit.starter.api.AuditEvent;
import com.shared.audit.starter.api.AuditService;
import com.shared.audit.starter.api.annotations.Audited;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

@Aspect
public class AuditAspect {
  private final AuditService audit;
  public AuditAspect(AuditService audit) { this.audit = audit; }

  @Around("@annotation(com.shared.audit.starter.api.annotations.Audited)")
  public Object auditMethod(ProceedingJoinPoint pjp) throws Throwable {
    Method m = ((MethodSignature) pjp.getSignature()).getMethod();
    Audited a = m.getAnnotation(Audited.class);
    Object result = null;
    Throwable error = null;
    try {
      result = pjp.proceed();
      return result;
    } catch (Throwable t) {
      error = t; throw t;
    } finally {
      AuditEvent.Builder b = AuditEvent.builder()
          .action(a.action())
          .message(a.message())
          .entity(a.entity(), null);
      if (error != null) b.outcome(com.shared.audit.starter.api.AuditOutcome.FAILURE).message(error.getMessage());
      audit.emit(b.build());
    }
  }
}
