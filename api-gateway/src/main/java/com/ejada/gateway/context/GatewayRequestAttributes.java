package com.ejada.gateway.context;

/**
 * Keys for {@link org.springframework.web.server.ServerWebExchange#getAttributes()}
 * used across gateway filters. Centralising the constants avoids string typos
 * when different components need to share contextual information such as the
 * resolved tenant identifier or correlation id.
 */
public final class GatewayRequestAttributes {

  /** Attribute storing the resolved tenant identifier. */
  public static final String TENANT_ID = GatewayRequestAttributes.class.getName() + ".tenantId";

  /** Attribute storing the correlation identifier for the current request. */
  public static final String CORRELATION_ID = GatewayRequestAttributes.class.getName() + ".correlationId";

  /** Attribute storing the cached subscription status for the current tenant. */
  public static final String SUBSCRIPTION = GatewayRequestAttributes.class.getName() + ".subscription";

  private GatewayRequestAttributes() {
    // utility class
  }
}

