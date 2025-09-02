package com.ejada.tenant.api.client;

import com.ejada.tenant.api.dto.EnforcementResultDto;
import com.ejada.tenant.api.dto.SubscriptionDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client for interacting with the policy service.
 */
@HttpExchange("/policy")
public interface PolicyClient {

  /**
   * Request policy enforcement for a subscription.
   *
   * @param subscription subscription data for enforcement
   * @return result of the enforcement check
   */
  @PostExchange("/enforce")
  EnforcementResultDto enforce(@RequestBody SubscriptionDto subscription);
}

