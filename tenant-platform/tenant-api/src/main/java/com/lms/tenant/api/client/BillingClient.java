package com.lms.tenant.api.client;

import com.lms.tenant.api.dto.OverageDto;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client for interacting with the billing service.
 */
@HttpExchange("/billing")
public interface BillingClient {

  /**
   * Report an overage to the billing system.
   *
   * @param overage details of the overage
   */
  @PostExchange("/overages")
  void reportOverage(@RequestBody OverageDto overage);
}

