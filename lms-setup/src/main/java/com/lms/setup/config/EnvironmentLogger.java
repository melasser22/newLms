package com.lms.setup.config;

import com.shared.config.EnvironmentProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Logs basic environment information at startup using values supplied by
 * {@link EnvironmentProperties}. This demonstrates centralized configuration
 * consumption and makes it easy to verify which environment the service is
 * running in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvironmentLogger {

    private final EnvironmentProperties properties;

    @PostConstruct
    void logEnv() {
        log.info("Environment: {} | Version: {}", properties.getEnvironment(), properties.getVersion());
    }
}

