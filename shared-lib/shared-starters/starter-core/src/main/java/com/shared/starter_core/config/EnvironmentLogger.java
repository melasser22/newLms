package com.shared.starter_core.config;

import com.shared.config.EnvironmentProperties;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logs basic environment information at startup using values supplied by
 * {@link EnvironmentProperties}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentLogger {

    @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Injected configuration is managed by Spring")
    private final EnvironmentProperties properties;

    @PostConstruct
    void logEnv() {
        log.info("Environment: {} | Version: {}", properties.getEnvironment(), properties.getVersion());
    }
}
