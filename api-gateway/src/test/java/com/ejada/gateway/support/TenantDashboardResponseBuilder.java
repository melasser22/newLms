package com.ejada.gateway.support;

import com.ejada.gateway.bff.TenantDashboardResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

/**
 * Convenience builder for composing {@link TenantDashboardResponse} objects in
 * tests. Many scenarios only care about a subset of fields so the builder
 * provides fluent defaults to keep test setup focused on behaviour.
 */
public final class TenantDashboardResponseBuilder {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private JsonNode tenant = objectMapper.createObjectNode().put("tenant", "Acme");
  private JsonNode usage = objectMapper.createObjectNode().put("cpu", 10);
  private JsonNode adoption = objectMapper.createObjectNode().putArray("features");
  private JsonNode cost = objectMapper.createObjectNode().put("forecast", 100);
  private JsonNode consumption = objectMapper.createObjectNode().put("plan", "gold");
  private List<String> warnings = List.of();

  public static TenantDashboardResponseBuilder tenantDashboardResponse() {
    return new TenantDashboardResponseBuilder();
  }

  private TenantDashboardResponseBuilder() {
  }

  public TenantDashboardResponseBuilder tenant(JsonNode tenant) {
    this.tenant = tenant;
    return this;
  }

  public TenantDashboardResponseBuilder usage(JsonNode usage) {
    this.usage = usage;
    return this;
  }

  public TenantDashboardResponseBuilder adoption(JsonNode adoption) {
    this.adoption = adoption;
    return this;
  }

  public TenantDashboardResponseBuilder cost(JsonNode cost) {
    this.cost = cost;
    return this;
  }

  public TenantDashboardResponseBuilder consumption(JsonNode consumption) {
    this.consumption = consumption;
    return this;
  }

  public TenantDashboardResponseBuilder warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public TenantDashboardResponse build() {
    return new TenantDashboardResponse(tenant, usage, adoption, cost, consumption, warnings);
  }
}
