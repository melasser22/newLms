package com.ejada.starter_data.config;

import com.ejada.starter_data.tenant.TenantHibernateFilterAspect;
import jakarta.persistence.EntityManagerFactory;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Bootstraps tenant filtering in Hibernate. */
@AutoConfiguration
@EnableConfigurationProperties(StarterDataTenantProps.class)
@ConditionalOnClass({Aspect.class, Session.class})
public class StarterDataTenantAutoConfiguration {

    @Bean
    @ConditionalOnBean(EntityManagerFactory.class)
    public TenantHibernateFilterAspect tenantHibernateFilterAspect(EntityManagerFactory emf,
                                                                   StarterDataTenantProps props) {
        return new TenantHibernateFilterAspect(emf, props);
    }
}
