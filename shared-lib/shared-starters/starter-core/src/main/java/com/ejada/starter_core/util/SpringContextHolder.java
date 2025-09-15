package com.ejada.starter_core.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Utility to access Spring-managed beans from non-managed code.
 * Should be used sparingly (prefer constructor injection).
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext ctx) throws BeansException {
        context = ctx;
    }

    /**
     * Get the Spring application context.
     */
    public static ApplicationContext getContext() {
        return context;
    }

    /**
     * Get a bean by type.
     */
    public static <T> T getBean(Class<T> type) {
        return context.getBean(type);
    }

    /**
     * Get a bean by name and type.
     */
    public static <T> T getBean(String name, Class<T> type) {
        return context.getBean(name, type);
    }

    /**
     * Clear the context (e.g., in tests).
     */
    public static void clear() {
        context = null;
    }
}
