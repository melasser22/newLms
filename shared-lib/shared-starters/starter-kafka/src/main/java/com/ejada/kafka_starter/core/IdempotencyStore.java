// src/main/java/com/shared/kafka/starter/core/IdempotencyStore.java
package com.ejada.kafka_starter.core;

/** Simple idempotency contract: returns true if first time seeing (group,messageId). */
public interface IdempotencyStore {
    /**
     * @param consumerGroup Kafka consumer group id
     * @param messageId unique id per message (header x-msg-id, or fallback topic-partition-offset)
     * @param ttlSeconds time to remember the message as processed
     * @return true if stored now (first time), false if duplicate
     */
    boolean putIfAbsent(String consumerGroup, String messageId, long ttlSeconds);
}
