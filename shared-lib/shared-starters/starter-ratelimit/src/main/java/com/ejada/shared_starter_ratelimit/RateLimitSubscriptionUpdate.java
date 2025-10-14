package com.ejada.shared_starter_ratelimit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RateLimitSubscriptionUpdate(
    @JsonProperty("tenantId") String tenantId,
    @JsonProperty("tier") String tier,
    @JsonProperty("requestsPerMinute") Integer requestsPerMinute,
    @JsonProperty("burstCapacity") Integer burstCapacity) {
}
