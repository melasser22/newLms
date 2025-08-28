package com.shared.audit.starter.api.context;
public record RequestInfo(String ip, String userAgent, String path, String method) {}
