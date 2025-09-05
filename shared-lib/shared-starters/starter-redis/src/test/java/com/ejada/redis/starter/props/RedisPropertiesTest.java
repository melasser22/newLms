package com.ejada.redis.starter.props;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Basic sanity tests for the Lombok-backed {@link RedisProperties} class.
 */
class RedisPropertiesTest {

  @Test
  void builderSetsValues() {
    RedisProperties props = RedisProperties.builder()
        .host("example.com")
        .port(1234)
        .build();

    assertEquals("example.com", props.getHost());
    assertEquals(1234, props.getPort());
  }
}

