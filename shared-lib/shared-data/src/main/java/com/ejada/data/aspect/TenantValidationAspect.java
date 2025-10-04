package com.ejada.data.aspect;

import com.ejada.starter_core.context.TenantContextResolver;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AOP aspect that validates tenant context before repository operations.
 */
@Aspect
@Component
public class TenantValidationAspect {

    private static final Logger log = LoggerFactory.getLogger(TenantValidationAspect.class);

    @Before("execution(* com.ejada.data.repository.TenantAwareRepository+.*Secure(..))")
    public void validateTenantContext(JoinPoint joinPoint) {
        UUID tenantId = TenantContextResolver.requireTenantId();
        if (log.isTraceEnabled()) {
            log.trace("Tenant context validated: {} for {}", tenantId, joinPoint.getSignature().toShortString());
        }
    }

    @Before("execution(* com.ejada.data.repository.TenantAwareRepository+.*Unsafe(..))")
    public void logUnsafeAccess(JoinPoint joinPoint) {
        log.warn("UNSAFE repository method invoked: {}", joinPoint.getSignature().toShortString());
    }
}
