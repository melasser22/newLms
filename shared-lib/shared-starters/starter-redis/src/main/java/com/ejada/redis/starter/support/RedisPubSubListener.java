package com.ejada.redis.starter.support;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

/**
 * Base listener that decodes channel & message to Strings.
 * Register with a RedisMessageListenerContainer in your app.
 */
public abstract class RedisPubSubListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String topic = new String(message.getChannel(), StandardCharsets.UTF_8);
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        handle(topic, payload);
    }

    /**
     * Called when a message arrives on a subscribed topic.
     */
    public abstract void handle(String topic, String payload);
}
