package com.common.dto;

import java.time.Instant;

/**
 * Standard metadata included with every request for traceability, auditing, and
 * multi-tenancy.
 */
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
    private Instant requestTime;

    // ===== Constructors =====
    public RequestMetadata() {
        this.requestTime = Instant.now();
    }

    public RequestMetadata(String traceId, String tenantId, String userId, String userName,
            String channel, String clientIp, String userAgent) {
        this.traceId = traceId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.userName = userName;
        this.channel = channel;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
        this.requestTime = Instant.now();
    }

    // ===== Getters & Setters =====
    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Instant requestTime) {
        this.requestTime = requestTime;
    }
}
