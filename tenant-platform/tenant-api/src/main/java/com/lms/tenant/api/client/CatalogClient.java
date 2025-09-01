package com.lms.tenant.api.client;

import com.lms.tenant.api.dto.EffectiveFeatureDto;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Declarative HTTP client for interacting with the catalog service.
 */
@HttpExchange("/catalog")
public interface CatalogClient {

  /**
   * Retrieve features available for a particular plan.
   *
   * @param planId identifier of the plan
   * @return list of features exposed by the plan
   */
  @GetExchange("/plans/{planId}/features")
  List<EffectiveFeatureDto> featuresForPlan(@PathVariable("planId") String planId);
}

