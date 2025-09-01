package com.lms.tenant.tenancy;

import java.util.Optional;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

public class TenantResolver {
    public Optional<UUID> resolveTenant(HttpServletRequest request) {
        String header = request.getHeader("X-Tenant-ID");
        if (header != null && !header.isBlank()) {
            return parse(header);
        }
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String sub = host.split("\\.")[0];
            return parse(sub);
        }
        return Optional.empty();
    }

    private Optional<UUID> parse(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
