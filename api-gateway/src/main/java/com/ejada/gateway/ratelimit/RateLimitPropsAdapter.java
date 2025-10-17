package com.ejada.gateway.ratelimit;

import com.ejada.shared_starter_ratelimit.RateLimitProps;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Backwards compatible accessor for {@link RateLimitProps}. The shared starter recently
 * refactored the rate limiting configuration to support multi-dimensional strategies,
 * removing legacy getters such as {@code getKeyStrategy()}, {@code getCapacity()} and
 * {@code getAlgorithm()}. The gateway still needs the derived values, so this helper
 * resolves them reflectively when the legacy methods are absent.
 */
public final class RateLimitPropsAdapter {

  private static final String DEFAULT_STRATEGY = "tenant";
  private static final String DEFAULT_ALGORITHM = "fixed";
  private static final int DEFAULT_CAPACITY = 60;

  private static final Method LEGACY_GET_KEY_STRATEGY =
      findMethod(RateLimitProps.class, "getKeyStrategy");
  private static final Method LEGACY_GET_CAPACITY =
      findMethod(RateLimitProps.class, "getCapacity");
  private static final Method LEGACY_GET_ALGORITHM =
      findMethod(RateLimitProps.class, "getAlgorithm");

  private static final Method GET_MULTIDIMENSIONAL =
      findMethod(RateLimitProps.class, "getMultidimensional");
  private static final Method GET_TIERS = findMethod(RateLimitProps.class, "getTiers");
  private static final Method GET_DEFAULT_TIER = findMethod(RateLimitProps.class, "getDefaultTier");

  private RateLimitPropsAdapter() {
    // utility class
  }

  public static String keyStrategy(@Nullable RateLimitProps props) {
    if (props == null) {
      return DEFAULT_STRATEGY;
    }

    String direct = invokeString(props, LEGACY_GET_KEY_STRATEGY);
    if (StringUtils.hasText(direct)) {
      return normalizeStrategy(direct);
    }

    Object multidimensional = invoke(props, GET_MULTIDIMENSIONAL);
    if (multidimensional != null) {
      Method getStrategies = findMethod(multidimensional.getClass(), "getStrategies");
      Object strategies = invoke(multidimensional, getStrategies);
      if (strategies instanceof Iterable<?> iterable) {
        Iterator<?> iterator = iterable.iterator();
        while (iterator.hasNext()) {
          Object strategy = iterator.next();
          if (strategy == null) {
            continue;
          }
          boolean enabled = invokeBoolean(strategy, "isEnabled");
          if (!enabled) {
            continue;
          }
          String name = invokeString(strategy, "getName");
          if (StringUtils.hasText(name)) {
            return normalizeStrategy(name);
          }
        }
      }
    }
    return DEFAULT_STRATEGY;
  }

  public static int capacity(@Nullable RateLimitProps props) {
    if (props == null) {
      return DEFAULT_CAPACITY;
    }

    Integer direct = invokeInteger(props, LEGACY_GET_CAPACITY);
    if (direct != null && direct > 0) {
      return direct;
    }

    Object tiersObject = invoke(props, GET_TIERS);
    if (tiersObject instanceof Map<?, ?> tiers && !tiers.isEmpty()) {
      String defaultTier = invokeString(props, GET_DEFAULT_TIER);
      Object tierProps = findTier(tiers, defaultTier);
      Integer requests = invokeInteger(tierProps, "getRequestsPerMinute");
      if (requests != null && requests > 0) {
        return requests;
      }
      // Fall back to burst capacity if that is the only available value.
      Integer burst = invokeInteger(tierProps, "getBurstCapacity");
      if (burst != null && burst > 0) {
        return burst;
      }
    }
    return DEFAULT_CAPACITY;
  }

  public static String algorithm(@Nullable RateLimitProps props) {
    if (props == null) {
      return DEFAULT_ALGORITHM;
    }
    String direct = invokeString(props, LEGACY_GET_ALGORITHM);
    if (StringUtils.hasText(direct)) {
      return normalizeAlgorithm(direct);
    }
    return DEFAULT_ALGORITHM;
  }

  private static Object invoke(@Nullable Object target, @Nullable Method method) {
    if (target == null || method == null) {
      return null;
    }
    try {
      return method.invoke(target);
    } catch (IllegalAccessException | InvocationTargetException ex) {
      return null;
    }
  }

  private static String invokeString(@Nullable Object target, @Nullable Method method) {
    Object value = invoke(target, method);
    if (value instanceof String str) {
      return str;
    }
    return null;
  }

  private static String invokeString(@Nullable Object target, String methodName) {
    Method method = findMethod(target != null ? target.getClass() : null, methodName);
    return invokeString(target, method);
  }

  private static Integer invokeInteger(@Nullable Object target, @Nullable Method method) {
    Object value = invoke(target, method);
    if (value instanceof Integer intValue) {
      return intValue;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return null;
  }

  private static Integer invokeInteger(@Nullable Object target, String methodName) {
    Method method = findMethod(target != null ? target.getClass() : null, methodName);
    return invokeInteger(target, method);
  }

  private static boolean invokeBoolean(@Nullable Object target, String methodName) {
    Method method = findMethod(target != null ? target.getClass() : null, methodName);
    if (method == null) {
      return true;
    }
    Object value = invoke(target, method);
    if (value instanceof Boolean bool) {
      return bool;
    }
    if (value instanceof String str) {
      return Boolean.parseBoolean(str);
    }
    return true;
  }

  @Nullable
  private static Method findMethod(@Nullable Class<?> type, String name) {
    if (type == null) {
      return null;
    }
    try {
      Method method = type.getMethod(name);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }

  private static Object findTier(Map<?, ?> tiers, @Nullable String defaultTier) {
    if (!StringUtils.hasText(defaultTier)) {
      return tiers.values().iterator().next();
    }
    Object direct = tiers.get(defaultTier);
    if (direct != null) {
      return direct;
    }
    // Attempt case-insensitive lookup because the shared starter normalises keys.
    for (Map.Entry<?, ?> entry : tiers.entrySet()) {
      Object key = entry.getKey();
      if (key instanceof String keyStr
          && keyStr.equalsIgnoreCase(Objects.toString(defaultTier, ""))) {
        return entry.getValue();
      }
    }
    Iterator<?> iterator = tiers.values().iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  private static String normalizeStrategy(String raw) {
    String value = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    if (value.isEmpty()) {
      return DEFAULT_STRATEGY;
    }
    return value;
  }

  private static String normalizeAlgorithm(String raw) {
    String value = raw.trim().toLowerCase(Locale.ROOT);
    if (Objects.equals(value, "sliding")) {
      return "sliding";
    }
    return DEFAULT_ALGORITHM;
  }
}
