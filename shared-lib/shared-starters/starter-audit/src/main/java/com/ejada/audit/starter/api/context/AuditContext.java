package com.ejada.audit.starter.api.context;

import java.util.HashMap;
import java.util.Map;

/** Simple per-thread context; for Reactor see ReactiveAuditAspect */
public final class AuditContext {
  private static final ThreadLocal<Map<String, Object>> TL = ThreadLocal.withInitial(HashMap::new);
  private AuditContext() {} 
  public static void put(String key, Object value) { TL.get().put(key, value); }
  public static Object get(String key) { return TL.get().get(key); }
  public static void clear() { TL.remove(); }
}
