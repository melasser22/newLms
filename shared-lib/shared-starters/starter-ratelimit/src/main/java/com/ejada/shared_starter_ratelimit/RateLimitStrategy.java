package com.ejada.shared_starter_ratelimit;

import com.ejada.shared_starter_ratelimit.RateLimitProps.Dimension;
import java.util.List;

/**
 * Strategy defining which request dimensions participate in key construction.
 */
public record RateLimitStrategy(String name, List<Dimension> dimensions) {

  public RateLimitStrategy {
    name = name == null ? "strategy" : name;
    dimensions = dimensions == null ? List.of(Dimension.TENANT) : List.copyOf(dimensions);
  }
}
