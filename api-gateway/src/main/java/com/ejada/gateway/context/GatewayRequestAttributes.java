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
  public static final String SUBSCRIPTION_TIER = GatewayRequestAttributes.class.getName() + ".subscriptionTier";

  /** Attribute storing the mutated request used by upstream filters. */
  public static final String MUTATED_REQUEST = GatewayRequestAttributes.class.getName() + ".mutatedRequest";

  /** Attribute storing the resolved API version for versioned routes. */
  public static final String API_VERSION = GatewayRequestAttributes.class.getName() + ".apiVersion";

  /** Attribute storing the originally requested API version (if provided). */
  public static final String API_VERSION_REQUESTED = GatewayRequestAttributes.class.getName()
      + ".apiVersionRequested";

  /** Attribute storing how the API version was resolved (header, path, default, mapping). */
  public static final String API_VERSION_SOURCE = GatewayRequestAttributes.class.getName()
      + ".apiVersionSource";

  /** Attribute storing a tenant-specific preferred API version if available. */
  public static final String API_VERSION_PREFERENCE = GatewayRequestAttributes.class.getName()
      + ".apiVersionPreference";

  /** Attribute indicating the transformation route id for compatibility bridging. */
  public static final String API_VERSION_TRANSFORMATION_ROUTE = GatewayRequestAttributes.class.getName()
      + ".apiVersionTransformationRoute";

  private GatewayRequestAttributes() {
    // utility class
  }
}

