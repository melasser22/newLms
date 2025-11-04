package com.ejada.sec.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {

    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(ApplicationContext applicationContext) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setApplicationContext(applicationContext);
        // Explicitly register the BeanFactory so SpEL expressions such as
        // "@authorizationExpressions" can resolve shared security helpers.
        handler.setBeanFactory(applicationContext);
        return handler;
    }
}
