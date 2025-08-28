package com.shared.redis.starter.support;

import org.springframework.data.redis.core.RedisTemplate;

import java.util.Objects;

/**
 * Simple publisher for Redis Pub/Sub.
 * Uses the value serializer of the provided template for payloads.
 */
public class RedisPubSubPublisher {

    private final RedisTemplate<String, Object> template;

    public RedisPubSubPublisher(RedisTemplate<String, Object> template) {
        this.template = Objects.requireNonNull(template);
    }

    /** Publish a string payload. */
    public void publish(String topic, String message) {
        template.convertAndSend(topic, message);
    }

    /** Publish any object payload (serialized via template's value serializer). */
    public void publish(String topic, Object payload) {
        template.convertAndSend(topic, payload);
    }
}
