package com.shared.audit.starter.core.mask;

import java.util.Map;

public final class FieldMasker {
  private FieldMasker() { }
  public static void maskKeys(Map<String,Object> map, Iterable<String> keys) {
    if (map==null) return;
    for (String k: keys) {
      if (map.containsKey(k)) map.put(k, "***");
    }
  }
}
