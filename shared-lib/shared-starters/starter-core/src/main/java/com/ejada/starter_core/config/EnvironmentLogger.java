package com.ejada.starter_core.config;

import com.ejada.config.EnvironmentProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Logs basic environment information at startup using values supplied by
 * {@link EnvironmentProperties}.
 */
@Component
public class EnvironmentLogger {

    private static final Logger log = LoggerFactory.getLogger(EnvironmentLogger.class);

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected configuration is managed by Spring")
    private final EnvironmentProperties properties;

    public EnvironmentLogger(EnvironmentProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void logEnv() {
        log.info("Environment: {} | Version: {}", properties.getEnvironment(), properties.getVersion());
    }
}
