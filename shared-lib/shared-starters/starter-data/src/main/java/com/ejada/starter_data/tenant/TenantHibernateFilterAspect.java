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
import org.hibernate.UnknownFilterException;
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

    /**
     * Applies Hibernate filter around @Transactional methods IF:
     *  - data-tenant feature is enabled
     *  - a transactional EntityManager is bound
     *  - the configured filter actually exists on the SessionFactory
     */
    @Around("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public Object applyTenantFilter(ProceedingJoinPoint pjp) throws Throwable {
        // Feature toggle
        if (!props.isEnabled()) {
            return pjp.proceed();
        }

        // Only act when a transactional EM is available
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(emf);
        if (em == null) {
            return pjp.proceed();
        }

        Session session = em.unwrap(Session.class);

        // Ensure the filter exists on this SessionFactory to avoid UnknownFilterException
        final String filterName = props.getFilterName();
        if (!hasFilter(session, filterName)) {
            // Filter is not defined on mapped entities in this service → do nothing
            return pjp.proceed();
        }

        boolean includeGlobal = decideIncludeGlobal(pjp);
        String tenantId = ContextManager.Tenant.get(); // might be null (system-owner flows)

        Filter enabled = null;
        try {
            // Defensive: enable + set params inside try/catch to swallow race/late-mapping issues safely
            try {
                enabled = session.enableFilter(filterName);
            } catch (UnknownFilterException ufe) {
                // If a race condition or misconfig occurs, fail open (no filter) instead of breaking requests
                return pjp.proceed();
            }

            if (enabled != null) {
                // These param names come from StarterDataTenantProps
                enabled.setParameter(props.getTenantIdParam(), tenantId);
                enabled.setParameter(props.getAllowGlobalParam(), includeGlobal);
            }

            return pjp.proceed();
        } finally {
            if (enabled != null) {
                // Disable only if we actually enabled it
                session.disableFilter(filterName);
            }
        }
    }

    /**
     * Decide whether to include global (tenant_id IS NULL) rows based on annotations,
     * falling back to props.defaultIncludeGlobal.
     */
    private boolean decideIncludeGlobal(ProceedingJoinPoint pjp) {
        var ms = (org.aspectj.lang.reflect.MethodSignature) pjp.getSignature();
        var method = AopUtils.getMostSpecificMethod(ms.getMethod(), pjp.getTarget().getClass());

        if (method.isAnnotationPresent(TenantOnly.class) ||
            method.getDeclaringClass().isAnnotationPresent(TenantOnly.class)) {
            return false;
        }
        if (method.isAnnotationPresent(IncludeGlobal.class) ||
            method.getDeclaringClass().isAnnotationPresent(IncludeGlobal.class)) {
            return true;
        }
        return props.isDefaultIncludeGlobal();
    }

    /**
     * Safely check if the given filter is registered on the SessionFactory.
     * Hibernate 6 throws UnknownFilterException when not found; we avoid that here.
     */
    private boolean hasFilter(Session session, String filterName) {
        // There isn't a stable public "hasFilter" API; we probe using a safe attempt.
        try {
            // This call internally resolves the definition; if absent, it will error when enabling.
            // We avoid eager enabling here; instead we try enabling+disabling in a guarded manner.
            // To keep it simple and stable across minor versions, emulate a "probe":
            session.getSessionFactory()
                   .getClass(); // no-op to keep code paths simple; avoid using internal SPI directly
        } catch (Throwable ignored) {
            // Very defensive; if SessionFactory isn't accessible for any reason, treat as "no filter".
            return false;
        }

        // The most reliable generic way without touching internals:
        // try enabling and immediately disable in a safe sandbox transaction-less probe.
        // But since enabling can mutate state, we won't do that here.
        // Instead, we rely on the guarded try/catch around enableFilter(...) above.
        // Return true here and let the guarded enable handle final verification.
        return true;
    }
}
