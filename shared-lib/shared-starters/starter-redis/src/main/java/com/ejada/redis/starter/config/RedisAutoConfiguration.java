package com.ejada.redis.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ejada.redis.starter.props.RedisProperties;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.TimeoutOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;  
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import com.ejada.redis.starter.support.RedisCacheHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.*;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@EnableConfigurationProperties(RedisProperties.class)
@ConditionalOnClass({RedisTemplate.class, LettuceConnectionFactory.class})
@ConditionalOnProperty(prefix = "shared.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(name = "redisObjectMapper")
  public ObjectMapper redisObjectMapper() {
    ObjectMapper om = new ObjectMapper();
    om.registerModule(new JavaTimeModule());
    om.findAndRegisterModules();
    return om;
  }

  @Bean
  @ConditionalOnMissingBean
  public KeyPrefixStrategy keyPrefixStrategy(RedisProperties props) {
    return () -> (props.getKeyPrefix() == null ? "shared" : props.getKeyPrefix()) + ":public:";
  }

  @Bean
  @ConditionalOnMissingBean(RedisConnectionFactory.class)
  public RedisConnectionFactory redisConnectionFactory(RedisProperties props) {
    // ... unchanged ...
    RedisStandaloneConfiguration standalone;
    if (props.getUrl() != null && !props.getUrl().isBlank()) {
      URI uri = URI.create(props.getUrl());
      standalone = new RedisStandaloneConfiguration(uri.getHost(), (uri.getPort() == -1) ? 6379 : uri.getPort());
      if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
        String[] up = uri.getUserInfo().split(":", 2);
        if (up.length == 2 && !up[1].isBlank()) standalone.setPassword(up[1]);
      } else if (props.getPassword() != null && !props.getPassword().isBlank()) {
        standalone.setPassword(props.getPassword());
      }
      if (uri.getPath() != null && !uri.getPath().isBlank()) {
        try { standalone.setDatabase(Integer.parseInt(uri.getPath().replaceFirst("^/",""))); }
        catch (NumberFormatException ignored) { standalone.setDatabase(props.getDatabase()); }
      } else {
        standalone.setDatabase(props.getDatabase());
      }
    } else {
      standalone = new RedisStandaloneConfiguration(props.getHost(), props.getPort());
      if (props.getPassword() != null && !props.getPassword().isBlank()) standalone.setPassword(props.getPassword());
      standalone.setDatabase(props.getDatabase());
    }

    LettuceClientConfigurationBuilder client = LettuceClientConfiguration.builder()
        .clientOptions(ClientOptions.builder()
            .autoReconnect(true)
            .timeoutOptions(TimeoutOptions.enabled())
            .build())
        .commandTimeout(props.getTimeout() == null ? Duration.ofSeconds(5) : props.getTimeout());

    if (props.isSsl() || (props.getUrl() != null && props.getUrl().startsWith("rediss://"))) client.useSsl();
    if (props.getClientName() != null && !props.getClientName().isBlank()) client.clientName(props.getClientName());

    return new LettuceConnectionFactory(standalone, client.build());
  }

  @Bean
  @ConditionalOnMissingBean
  public RedisCacheHelper redisCacheHelper(
      RedisTemplate<String, Object> redisTemplate, KeyPrefixStrategy keyPrefixStrategy) {
    return new RedisCacheHelper(redisTemplate, keyPrefixStrategy);
  }

  // Serializers
  @Bean @ConditionalOnMissingBean(name = "redisKeySerializer")
  public RedisSerializer<String> redisKeySerializer() { return new StringRedisSerializer(); }

  @Bean @ConditionalOnMissingBean(name = "redisValueSerializer")
  public RedisSerializer<Object> redisValueSerializer(
      @Qualifier("redisObjectMapper") ObjectMapper om) {                 // <— qualify here
    return new GenericJackson2JsonRedisSerializer(om);
  }

  // Templates
  @Bean @ConditionalOnMissingBean
  public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) { return new StringRedisTemplate(cf); }

  @Bean(name = "redisTemplate") @ConditionalOnMissingBean(name = "redisTemplate")
  public RedisTemplate<String, Object> redisTemplate(
      RedisConnectionFactory cf,
      RedisSerializer<String> keySer,
      RedisSerializer<Object> valSer) {
    RedisTemplate<String,Object> t = new RedisTemplate<>();
    t.setConnectionFactory(cf);
    t.setKeySerializer(keySer); t.setHashKeySerializer(keySer);
    t.setValueSerializer(valSer); t.setHashValueSerializer(valSer);
    t.afterPropertiesSet();
    return t;
  }

  // Reactive template (opt-in)
  @Bean
  @ConditionalOnClass(ReactiveRedisTemplate.class)
  @ConditionalOnProperty(prefix = "shared.redis", name = "reactive", havingValue = "true")
  @ConditionalOnMissingBean
  public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
      ReactiveRedisConnectionFactory cf,
      @Qualifier("redisObjectMapper") ObjectMapper om) {                 // <— and here

    StringRedisSerializer keySer = new StringRedisSerializer();
    GenericJackson2JsonRedisSerializer valSer = new GenericJackson2JsonRedisSerializer(om);
    RedisSerializationContext<String,Object> ctx = RedisSerializationContext
        .<String,Object>newSerializationContext(keySer)
        .value(valSer).hashValue(valSer).build();
    return new ReactiveRedisTemplate<>(cf, ctx);
  }

  // Cache Manager
  @Bean
  @ConditionalOnClass(CacheManager.class)
  @ConditionalOnMissingBean(CacheManager.class)
  public CacheManager cacheManager(
      RedisConnectionFactory cf,
      RedisProperties props,
      KeyPrefixStrategy prefix,
      ObjectProvider<RedisCacheConfiguration> baseConfigProvider,
      RedisSerializer<String> keySer,
      RedisSerializer<Object> valSer) {

    RedisCacheConfiguration base = baseConfigProvider.getIfAvailable(() ->
        RedisCacheConfiguration.defaultCacheConfig()
          .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySer))
          .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valSer))
          .disableCachingNullValues()
          .computePrefixWith(name -> prefix.resolvePrefix() + name + "::")
          .entryTtl(props.getDefaultTtl() == null ? Duration.ofMinutes(10) : props.getDefaultTtl())
    );

    Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
    if (props.getCaches() != null) {
      props.getCaches().forEach((name, spec) -> {
        RedisCacheConfiguration cfg = base;
        if (spec.getTtl() != null) cfg = cfg.entryTtl(spec.getTtl());
        if (Boolean.TRUE.equals(spec.getCacheNulls())) {
          cfg = cfg.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valSer))
                   .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySer))
                   .computePrefixWith(base.getKeyPrefixFor(name)::concat);
        }
        if (spec.getKeyPrefixOverride() != null && !spec.getKeyPrefixOverride().isBlank()) {
          String override = spec.getKeyPrefixOverride();
          cfg = cfg.computePrefixWith(n -> override + n + "::");
        }
        perCache.put(name, cfg);
      });
    }

    return RedisCacheManager.builder(cf).cacheDefaults(base).withInitialCacheConfigurations(perCache).build();
  }
}
