package com.ejada.gateway.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;

/**
 * Defers the initialisation of infrastructure connections that should only begin once the
 * application is ready to handle traffic.
 */
@Component
@Lazy(false)
public class DeferredInfrastructureInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeferredInfrastructureInitializer.class);

  private final ObjectProvider<KafkaListenerEndpointRegistry> kafkaRegistryProvider;
  private final ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider;

  public DeferredInfrastructureInitializer(ObjectProvider<KafkaListenerEndpointRegistry> kafkaRegistryProvider,
      ObjectProvider<ReactiveStringRedisTemplate> redisTemplateProvider) {
    this.kafkaRegistryProvider = kafkaRegistryProvider;
    this.redisTemplateProvider = redisTemplateProvider;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    startKafkaListeners();
    warmRedisConnection();
  }

  private void startKafkaListeners() {
    KafkaListenerEndpointRegistry registry = kafkaRegistryProvider.getIfAvailable();
    if (registry == null) {
      LOGGER.debug("KafkaListenerEndpointRegistry not available; skipping deferred startup");
      return;
    }
    if (registry.isRunning()) {
      LOGGER.debug("Kafka listener containers already running; no deferred startup required");
      return;
    }
    registry.start();
    LOGGER.info("Started {} Kafka listener containers after ApplicationReadyEvent",
        registry.getListenerContainers().size());
  }

  private void warmRedisConnection() {
    ReactiveStringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
    if (redisTemplate == null || redisTemplate.getConnectionFactory() == null) {
      LOGGER.debug("ReactiveStringRedisTemplate not available; skipping Redis warm-up");
      return;
    }
    try (ReactiveRedisConnection connection = redisTemplate.getConnectionFactory().getReactiveConnection()) {
      connection.ping().block(Duration.ofSeconds(2));
      LOGGER.info("Verified Redis connectivity after ApplicationReadyEvent");
    } catch (Exception ex) {
      LOGGER.warn("Redis connectivity warm-up failed after ApplicationReadyEvent", ex);
    }
  }
}

