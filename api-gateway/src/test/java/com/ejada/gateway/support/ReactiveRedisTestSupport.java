package com.ejada.gateway.support;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ReactiveSetOperations;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

/**
 * Utility helpers to mock {@link ReactiveStringRedisTemplate} interactions in unit tests without
 * requiring a running Redis server or Testcontainers. The provided mocks capture values in a simple
 * in-memory map while supporting the limited operations exercised by the gateway security tests.
 */
public final class ReactiveRedisTestSupport {

  private ReactiveRedisTestSupport() {
  }

  /**
   * Creates a new concurrent store suitable for use with {@link #mockStringTemplate(Map)}.
   *
   * @return an empty concurrent map
   */
  public static InMemoryRedisStore newStore() {
    return new InMemoryRedisStore();
  }

  /**
   * Builds a mocked {@link ReactiveStringRedisTemplate} backed by the provided in-memory store.
   * Only a subset of Redis value operations are implemented – enough for the unit tests to verify
   * behaviour without external infrastructure.
   *
   * @param store backing store for key/value pairs
   * @return mocked {@link ReactiveStringRedisTemplate}
   */
  public static ReactiveStringRedisTemplate mockStringTemplate(InMemoryRedisStore store) {
    ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    ReactiveValueOperations<String, String> valueOperations = mock(ReactiveValueOperations.class);
    @SuppressWarnings("unchecked")
    ReactiveSetOperations<String, String> setOperations = mock(ReactiveSetOperations.class);

    when(template.opsForValue()).thenReturn(valueOperations);
    when(template.opsForSet()).thenReturn(setOperations);

    lenient().when(template.expire(anyString(), any(Duration.class)))
        .thenAnswer(invocation -> Mono.just(store.values.containsKey(invocation.getArgument(0))));

    lenient().when(valueOperations.set(anyString(), anyString()))
        .thenAnswer(invocation -> {
          store.values.put(invocation.getArgument(0), invocation.getArgument(1));
          return Mono.just(Boolean.TRUE);
        });

    lenient().when(valueOperations.set(anyString(), anyString(), any(Duration.class)))
        .thenAnswer(invocation -> {
          store.values.put(invocation.getArgument(0), invocation.getArgument(1));
          return Mono.just(Boolean.TRUE);
        });

    lenient().when(valueOperations.get(any()))
        .thenAnswer(invocation -> Mono.justOrEmpty(store.values.get(invocation.getArgument(0))));

    lenient().when(valueOperations.increment(anyString()))
        .thenAnswer(invocation -> {
          String key = invocation.getArgument(0);
          long current = 0L;
          String existing = store.values.get(key);
          if (existing != null) {
            try {
              current = Long.parseLong(existing);
            } catch (NumberFormatException ignored) {
              current = 0L;
            }
          }
          long updated = current + 1L;
          store.values.put(key, Long.toString(updated));
          return Mono.just(updated);
        });

    lenient().when(valueOperations.getAndSet(anyString(), anyString()))
        .thenAnswer(invocation -> {
          String key = invocation.getArgument(0);
          String value = invocation.getArgument(1);
          String previous = store.values.put(key, value);
          return Mono.justOrEmpty(previous);
        });

    lenient().when(setOperations.add(anyString(), any()))
        .thenAnswer(invocation -> {
          String key = invocation.getArgument(0);
          Object raw = invocation.getArgument(1);
          Object[] members = raw instanceof Object[] ? (Object[]) raw : new Object[] { raw };
          Set<String> set = store.sets.computeIfAbsent(key, unused -> ConcurrentHashMap.newKeySet());
          long added = Arrays.stream(members)
              .map(String::valueOf)
              .filter(member -> set.add(member))
              .count();
          return Mono.just(added);
        });

    lenient().when(setOperations.members(anyString()))
        .thenAnswer(invocation -> {
          String key = invocation.getArgument(0);
          Set<String> set = store.sets.get(key);
          if (set == null || set.isEmpty()) {
            return Flux.empty();
          }
          return Flux.fromIterable(set);
        });

    lenient().when(setOperations.isMember(anyString(), any()))
        .thenAnswer(invocation -> {
          String key = invocation.getArgument(0);
          String member = String.valueOf(invocation.getArgument(1));
          Set<String> set = store.sets.get(key);
          boolean present = set != null && set.contains(member);
          return Mono.just(present);
        });

    return template;
  }

  public static final class InMemoryRedisStore {
    private final ConcurrentMap<String, String> values = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> sets = new ConcurrentHashMap<>();

    public void clear() {
      values.clear();
      sets.clear();
    }
  }
}
