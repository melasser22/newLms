package com.ejada.gateway.dr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.data.redis.connection.RedisConnectionFailureEvent;
import org.springframework.stereotype.Component;

/**
 * Bridges Redis connection failures into the region failover service.
 */
@Component
public class RedisSentinelFailoverListener implements ApplicationListener<RedisConnectionFailureEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedisSentinelFailoverListener.class);

  private final RegionFailoverService failoverService;

  public RedisSentinelFailoverListener(RegionFailoverService failoverService) {
    this.failoverService = failoverService;
  }

  @Override
  public void onApplicationEvent(RedisConnectionFailureEvent event) {
    LOGGER.warn("Redis connection failure detected: {}", event.getCause() != null ? event.getCause().getMessage() : "unknown");
    failoverService.recordFailure(event.getCause() != null ? event.getCause() : new IllegalStateException("redis-connection"));
  }
}
