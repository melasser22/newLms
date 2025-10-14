package com.ejada.gateway.graphql;

/**
 * GraphQL representation of a catalog item available to a tenant.
 */
public record CatalogItemNode(String id, String name, String category, String description) {
}
