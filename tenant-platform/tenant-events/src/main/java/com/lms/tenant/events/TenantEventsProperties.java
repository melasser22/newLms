package com.lms.tenant.events;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tenant.events")
public class TenantEventsProperties {
    /** Whether tenant events are enabled. */
    private boolean enabled = true;
    /** Poll interval for publisher. */
    private Duration pollInterval = Duration.ofSeconds(5);
    /** Batch size for polling. */
    private int batchSize = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Duration pollInterval) {
        this.pollInterval = pollInterval;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
