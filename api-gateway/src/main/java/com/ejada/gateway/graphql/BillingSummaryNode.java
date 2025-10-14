package com.ejada.gateway.graphql;

/**
 * GraphQL representation of the billing snapshot for a tenant.
 */
public record BillingSummaryNode(String currency, double totalDue, double usage, String nextInvoiceDate) {
}
