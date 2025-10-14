package com.ejada.gateway.graphql;

/**
 * GraphQL view of a tenant enriched by downstream services via data loaders.
 */
public record TenantNode(Integer id, String code, String name, String status) {
}
