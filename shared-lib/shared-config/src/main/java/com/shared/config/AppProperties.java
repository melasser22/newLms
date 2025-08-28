package com.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Application-level configuration properties that can be centrally managed and
 * injected into services. Defaults provide sane values for local development.
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** Current environment (dev, staging, prod). */
    private String env = "dev";
    /** Service version for diagnostics. */
    private String version = "1.0.0";
}
