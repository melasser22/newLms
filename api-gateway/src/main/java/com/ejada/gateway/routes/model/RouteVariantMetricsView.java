package com.ejada.gateway.routes.model;

import java.util.UUID;

/**
 * Projection of runtime traffic split metrics exposed for the administration UI.
 */
public record RouteVariantMetricsView(
    UUID routeId,
    String variantId,
    String gatewayRouteId,
    boolean canary,
    long requests,
    long successes,
    long errors,
    long conversions,
    double errorRate,
    double conversionRate) {
}
