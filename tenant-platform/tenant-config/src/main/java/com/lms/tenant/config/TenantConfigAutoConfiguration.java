package com.lms.tenant.config;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Auto configuration wiring tenant resolution and datasource interception.
 */
@AutoConfiguration
public class TenantConfigAutoConfiguration {

    @Bean
    public TenantResolver tenantResolver() {
        return new TenantResolver();
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilter(TenantResolver tenantResolver) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>(new TenantFilter(tenantResolver));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    public static BeanPostProcessor tenantDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof DataSource dataSource && !(dataSource instanceof DelegatingDataSource)) {
                    return new TenantConnectionInterceptor(dataSource);
                }
                return bean;
            }
        };
    }
}
