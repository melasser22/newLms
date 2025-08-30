package com.lms.setup.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for security options specific to the setup service.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "setup.security")
public class SetupSecurityProperties {

    /**
     * Enables or disables role checks on API endpoints.
     */
    private boolean enableRoleCheck = true;
}
