package com.ejada.tenant.events;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties(prefix = "tenant.events")
public class TenantEventsProperties {
    /** Whether tenant events are enabled. */
    private boolean enabled = true;
    /** Poll interval for publisher. */
    private Duration pollInterval = Duration.ofSeconds(5);
    /** Batch size for polling. */
    private int batchSize = 10;
}
