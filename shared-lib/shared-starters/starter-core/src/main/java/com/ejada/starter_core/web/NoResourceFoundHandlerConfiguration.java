package com.ejada.starter_core.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Registers {@link NoResourceFoundExceptionHandler} only when the MVC-specific
 * {@code NoResourceFoundException} is present on the classpath.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.web.servlet.resource.NoResourceFoundException")
@Import(NoResourceFoundExceptionHandler.class)
public class NoResourceFoundHandlerConfiguration {
}
