package com.ejada.gateway.routes.repository;

import com.ejada.gateway.config.RouteSchemaInitializer;
import com.ejada.gateway.routes.model.RouteDefinition;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class RouteDefinitionRepository {

  private static final Logger LOGGER = LoggerFactory.getLogger(RouteDefinitionRepository.class);
  private static final String ACTIVE_ROUTES_CACHE_KEY = "gateway:routes:active";

  private final RouteDefinitionR2dbcRepository routeStore;
  private final RouteDefinitionAuditR2dbcRepository auditStore;
  private final RouteDefinitionMapper mapper;
  private final ReactiveStringRedisTemplate redisTemplate;
  private final RouteSchemaInitializer schemaInitializer;

  public RouteDefinitionRepository(
      RouteDefinitionR2dbcRepository routeStore,
      RouteDefinitionAuditR2dbcRepository auditStore,
      RouteDefinitionMapper mapper,
      ReactiveStringRedisTemplate redisTemplate,
      RouteSchemaInitializer schemaInitializer) {
    this.routeStore = routeStore;
    this.auditStore = auditStore;
    this.mapper = mapper;
    this.redisTemplate = redisTemplate;
    this.schemaInitializer = schemaInitializer;
  }

  public Flux<RouteDefinition> findAll() {
    return routeStore.findAll().map(mapper::toDomain);
  }

  public Mono<RouteDefinition> findById(UUID id) {
    return routeStore.findById(id)
        .switchIfEmpty(Mono.error(new RouteNotFoundException(id)))
        .map(mapper::toDomain);
  }

  public Flux<RouteDefinition> findActiveRoutes() {
    return readCache()
        .switchIfEmpty(loadAndCacheActiveRoutes());
  }

  @Transactional
  public Mono<RouteDefinition> create(RouteDefinition definition, String actor) {
    Instant now = Instant.now();
    RouteDefinition initialised = definition
        .withId(definition.id() == null ? UUID.randomUUID() : definition.id())
        .withVersion(Math.max(1, definition.version()), now)
        .withTimestamps(now, now);
    RouteDefinitionEntity entity = mapper.toEntity(initialised);
    return routeStore.save(entity)
        .map(mapper::toDomain)
        .flatMap(saved -> audit("CREATE", saved, actor).thenReturn(saved))
        .flatMap(saved -> evictCache().thenReturn(saved));
  }

  @Transactional
  public Mono<RouteDefinition> update(RouteDefinition definition, String actor) {
    UUID id = definition.requireId().id();
    return routeStore.findById(id)
        .switchIfEmpty(Mono.error(new RouteNotFoundException(id)))
        .map(mapper::toDomain)
        .flatMap(existing -> {
          Instant now = Instant.now();
          RouteDefinition updated = definition
              .withVersion(existing.version() + 1, now)
              .withTimestamps(existing.createdAt(), now);
          return routeStore.save(mapper.toEntity(updated))
              .map(mapper::toDomain)
              .flatMap(saved -> audit("UPDATE", saved, actor).thenReturn(saved));
        })
        .flatMap(saved -> evictCache().thenReturn(saved));
  }

  @Transactional
  public Mono<RouteDefinition> disable(UUID id, String actor) {
    return routeStore.findById(id)
        .switchIfEmpty(Mono.error(new RouteNotFoundException(id)))
        .map(mapper::toDomain)
        .flatMap(existing -> {
          Instant now = Instant.now();
          RouteDefinition disabled = existing
              .withState(false, now)
              .withVersion(existing.version() + 1, now);
          RouteDefinitionEntity entity = mapper.toEntity(disabled);
          return routeStore.save(entity)
              .map(mapper::toDomain)
              .flatMap(saved -> audit("DISABLE", saved, actor).thenReturn(saved));
        })
        .flatMap(saved -> evictCache().thenReturn(saved));
  }

  private Mono<Void> evictCache() {
    return redisTemplate.delete(ACTIVE_ROUTES_CACHE_KEY)
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to evict route cache", ex);
          return Mono.empty();
        })
        .then();
  }

  private Flux<RouteDefinition> readCache() {
    return redisTemplate.opsForValue().get(ACTIVE_ROUTES_CACHE_KEY)
        .flatMapMany(payload -> {
          if (!StringUtils.hasText(payload)) {
            return Flux.empty();
          }
          try {
            List<RouteDefinition> cached = mapper.decodeList(payload);
            return Flux.fromIterable(cached);
          } catch (Exception ex) {
            LOGGER.warn("Failed to decode route cache", ex);
            return Flux.empty();
          }
        })
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to read route cache", ex);
          return Flux.empty();
        });
  }

  private Flux<RouteDefinition> loadAndCacheActiveRoutes() {
    return schemaInitializer.ensureSchema()
        .thenMany(routeStore.findAllByEnabledTrue())
        .map(mapper::toDomain)
        .collectList()
        .flatMapMany(list -> cacheActiveRoutes(list).thenMany(Flux.fromIterable(list)))
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to load active routes from database", ex);
          return Flux.empty();
        });
  }

  private Mono<Void> cacheActiveRoutes(List<RouteDefinition> routes) {
    if (routes.isEmpty()) {
      return redisTemplate.delete(ACTIVE_ROUTES_CACHE_KEY).then();
    }
    String payload = mapper.encodeList(routes);
    return redisTemplate.opsForValue().set(ACTIVE_ROUTES_CACHE_KEY, payload)
        .onErrorResume(DataAccessException.class, ex -> {
          LOGGER.warn("Failed to write route cache", ex);
          return Mono.just(Boolean.FALSE);
        })
        .then();
  }

  private Mono<Void> audit(String changeType, RouteDefinition route, String actor) {
    return auditStore.save(mapper.toAuditEntity(route, changeType, actor))
        .then()
        .onErrorResume(ex -> {
          LOGGER.warn("Failed to persist audit entry for route {}", route.id(), ex);
          return Mono.empty();
        });
  }
}
