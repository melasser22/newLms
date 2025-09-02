package com.ejada.tenant.api.client;

import com.ejada.tenant.api.dto.SubscriptionDto;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for interacting with the subscription service.
 */
@HttpExchange("/subscriptions")
public interface SubscriptionClient {

  /**
   * Retrieve subscription information for a tenant.
   *
   * @param tenantId identifier of the tenant
   * @return the tenant's subscription details
   */
  @GetExchange("/{tenantId}")
  SubscriptionDto findByTenant(@PathVariable("tenantId") UUID tenantId);
}

