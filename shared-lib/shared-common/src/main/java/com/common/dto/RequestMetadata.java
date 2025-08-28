package com.common.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Standard metadata included with every request for traceability, auditing, and
 * multi-tenancy.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetadata {

    /** Unique request correlation ID (traceId) */
    private String traceId;

    /** Tenant identifier (for multi-tenancy) */
    private String tenantId;

    /** User ID of the authenticated user */
    private String userId;

    /** Username or display name of the authenticated user */
    private String userName;

    /** Source system / channel (e.g., WEB, MOBILE, API) */
    private String channel;

    /** Client IP address */
    private String clientIp;

    /** User agent (browser, app, etc.) */
    private String userAgent;

    /** Timestamp when request was received */
    @Builder.Default
    private Instant requestTime = Instant.now();
}
