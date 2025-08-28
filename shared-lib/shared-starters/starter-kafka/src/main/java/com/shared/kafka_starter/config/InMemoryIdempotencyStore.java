package com.shared.kafka_starter.config;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.shared.kafka_starter.core.IdempotencyStore;


public class InMemoryIdempotencyStore implements IdempotencyStore {
  private final Map<String, Long> seen = new ConcurrentHashMap<>();
  @Override public boolean putIfAbsent(String group, String msgId, long ttlSeconds) {
    long now = Instant.now().getEpochSecond();
    String key = group + ":" + msgId;
    return seen.putIfAbsent(key, now + ttlSeconds) == null;
  }
}