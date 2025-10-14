package com.ejada.gateway.graphql;

/**
 * GraphQL representation of a subscription returned as part of the unified tenant schema.
 */
public record SubscriptionNode(String id, String status, String product, Integer seats, String startedAt) {
}
