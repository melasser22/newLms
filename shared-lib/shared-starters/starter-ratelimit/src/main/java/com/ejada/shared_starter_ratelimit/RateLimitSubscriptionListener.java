package com.ejada.shared_starter_ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.util.StringUtils;

/**
 * Listens for tenant subscription updates via Redis pub/sub.
 */
public class RateLimitSubscriptionListener extends MessageListenerAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitSubscriptionListener.class);

  private final RedisMessageListenerContainer container;
  private final TenantRateLimitRegistry registry;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String channel;

  public RateLimitSubscriptionListener(RedisConnectionFactory connectionFactory, String channel,
      TenantRateLimitRegistry registry) {
    this.container = new RedisMessageListenerContainer();
    this.container.setConnectionFactory(connectionFactory);
    this.container.afterPropertiesSet();
    this.registry = registry;
    this.channel = channel;
  }

  public void start() {
    container.addMessageListener((MessageListener) this::onMessage, ChannelTopic.of(channel));
    container.start();
  }

  public void stop() {
    container.stop();
  }

  @Override
  public void onMessage(Message message, byte[] pattern) {
    String payload = new String(message.getBody(), StandardCharsets.UTF_8);
    if (!StringUtils.hasText(payload)) {
      return;
    }
    try {
      RateLimitSubscriptionUpdate update = objectMapper.readValue(payload, RateLimitSubscriptionUpdate.class);
      if (update.tenantId() == null) {
        LOGGER.warn("Ignoring rate limit update without tenant id: {}", payload);
        return;
      }
      registry.apply(update);
      LOGGER.info("Updated rate limit tier for tenant {}", update.tenantId());
    } catch (Exception ex) {
      LOGGER.error("Failed to process rate limit update message: {}", payload, ex);
    }
  }

  public RedisMessageListenerContainer getContainer() {
    return container;
  }
}
