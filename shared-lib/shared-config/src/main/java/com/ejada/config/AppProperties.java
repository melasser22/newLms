package com.ejada.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.cloud.context.config.annotation.RefreshScope;

/**
 * Application-level configuration properties that can be centrally managed and
 * injected into services. Defaults provide sane values for local development.
 */
@Data
@RefreshScope
@Validated
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** Current environment (dev, staging, prod). */
    @NotBlank(message = "Application environment must be provided")
    @Pattern(
        regexp = "(?i)local|dev|staging|production",
        message = "Environment must be one of local, dev, staging or production")
    private String env = "dev";
    /** Service version for diagnostics. */
    @NotBlank(message = "Service version must be provided")
    private String version = "1.0.0";
    /** Monotonically increasing configuration reload counter. */
    @Min(value = 1, message = "Configuration version must be at least 1")
    private long configurationVersion = 1L;
}
