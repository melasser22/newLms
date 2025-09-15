package com.ejada.starter_data.tenant;

import com.ejada.common.context.ContextManager;
import com.ejada.starter_data.config.StarterDataTenantProps;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.aop.support.AopUtils;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

@Aspect
public class TenantHibernateFilterAspect {

    private final EntityManagerFactory emf;
    private final StarterDataTenantProps props;

    public TenantHibernateFilterAspect(EntityManagerFactory emf, StarterDataTenantProps props) {
        this.emf = emf;
        this.props = props;
    }

    // Around all @Transactional methods
    @Around("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantFilter(ProceedingJoinPoint pjp) throws Throwable {
        if (!props.isEnabled()) return pjp.proceed();

        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
        if (em == null) return pjp.proceed(); // no bound EM

        Session session = em.unwrap(Session.class);

        boolean includeGlobal = decideIncludeGlobal(pjp);
        String tenantId = ContextManager.Tenant.get(); // may be null

        Filter f = session.enableFilter(props.getFilterName());
        f.setParameter(props.getTenantIdParam(), tenantId);
        f.setParameter(props.getAllowGlobalParam(), includeGlobal);

        try {
            return pjp.proceed();
        } finally {
            session.disableFilter(props.getFilterName());
        }
    }

    private boolean decideIncludeGlobal(ProceedingJoinPoint pjp) {
        var ms = (org.aspectj.lang.reflect.MethodSignature) pjp.getSignature();
        var method = AopUtils.getMostSpecificMethod(ms.getMethod(), pjp.getTarget().getClass());
        if (method.isAnnotationPresent(TenantOnly.class) || method.getDeclaringClass().isAnnotationPresent(TenantOnly.class))
            return false;
        if (method.isAnnotationPresent(IncludeGlobal.class) || method.getDeclaringClass().isAnnotationPresent(IncludeGlobal.class))
            return true;
        return props.isDefaultIncludeGlobal();
    }
}
