package com.ejada.starter_core.tenant;

import org.springframework.web.server.ServerWebExchange;

public interface TenantResolver {
    /** Return tenant id if found; else null. Must NOT throw. */
    String resolve(ServerWebExchange exchange);
}