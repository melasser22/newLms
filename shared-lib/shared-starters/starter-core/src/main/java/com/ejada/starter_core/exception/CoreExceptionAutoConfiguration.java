package com.ejada.starter_core.exception;

import com.ejada.starter_core.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration that registers the consolidated {@link GlobalExceptionHandler} from the
 * {@code web} package. This avoids duplicate handler definitions and ensures a single,
 * consistent exception mapping across applications.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.http.HttpStatusCode")
@Import(GlobalExceptionHandler.class)
public class CoreExceptionAutoConfiguration {
}
