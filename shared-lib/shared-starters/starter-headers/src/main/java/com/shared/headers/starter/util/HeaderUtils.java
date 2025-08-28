
package com.shared.headers.starter.util;

import org.slf4j.MDC;

import java.util.Map;
import java.util.UUID;

public final class HeaderUtils {
  private HeaderUtils() {}

  public static String firstNonEmpty(String... values) {
    if (values == null) return null;
    for (String v : values) {
      if (v != null && !v.isBlank()) return v;
    }
    return null;
  }

  public static String uuid() {
    return UUID.randomUUID().toString();
  }

  public static void putMdc(Map<String,String> kv) {
    if (kv == null) return;
    kv.forEach((k,v) -> {
      if (v != null) MDC.put(k, v);
    });
  }

  public static void clearMdc(String... keys) {
    if (keys == null) return;
    for (String k : keys) MDC.remove(k);
  }
}
