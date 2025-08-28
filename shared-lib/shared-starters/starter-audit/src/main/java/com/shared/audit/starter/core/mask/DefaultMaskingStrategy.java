package com.shared.audit.starter.core.mask;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backward-compatible masking strategy.
 * Supports:
 *  - new DefaultMaskingStrategy(Iterable<String> keys)
 *  - new DefaultMaskingStrategy(Map<String,String> fieldsByKey)
 *  - new DefaultMaskingStrategy(Map<String,String> fieldsByKey, List<String> jsonPaths)
 *  - static factories: of/from/create/build with (Map) and (Map,List) and (Iterable)
 *  - no-arg ctor (defaults to no keys)
 */
public class DefaultMaskingStrategy implements MaskingStrategy {

  private final Set<String> keys;
  // kept for signature compatibility; not used by the simple masking below
  @SuppressWarnings("unused")
  private final List<String> jsonPaths;

  /** No-arg for maximum compatibility (no masking by default). */
  public DefaultMaskingStrategy() {
    this(Collections.emptySet(), Collections.emptyList());
  }

  /** Original style ctor — keep working. */
  public DefaultMaskingStrategy(Iterable<String> keys) {
    this(keys, Collections.emptyList());
  }

  /** Convenience: accept Map<String,String> (values ignored; keys are the fields to mask). */
  public DefaultMaskingStrategy(Map<String, String> fieldsByKey) {
    this(fieldsByKey == null ? Collections.emptySet() : fieldsByKey.keySet(), Collections.emptyList());
  }

  /** Convenience: accept Map<String,String> + jsonPaths (paths currently unused but accepted). */
  public DefaultMaskingStrategy(Map<String, String> fieldsByKey, List<String> jsonPaths) {
    this(fieldsByKey == null ? Collections.emptySet() : fieldsByKey.keySet(), jsonPaths);
  }

  /** Internal canonical ctor. */
  private DefaultMaskingStrategy(Iterable<String> keys, List<String> jsonPaths) {
    this.keys = toSet(keys);
    this.jsonPaths = (jsonPaths == null) ? Collections.emptyList() : List.copyOf(jsonPaths);
  }

  // ---------- Static factories for wide compatibility ----------
  public static MaskingStrategy of(Map<String, String> fieldsByKey) {
    return new DefaultMaskingStrategy(fieldsByKey);
  }
  public static MaskingStrategy of(Map<String, String> fieldsByKey, List<String> jsonPaths) {
    return new DefaultMaskingStrategy(fieldsByKey, jsonPaths);
  }
  public static MaskingStrategy of(Iterable<String> keys) {
    return new DefaultMaskingStrategy(keys);
  }

  public static MaskingStrategy from(Map<String, String> fieldsByKey) { return of(fieldsByKey); }
  public static MaskingStrategy from(Map<String, String> fieldsByKey, List<String> jsonPaths) { return of(fieldsByKey, jsonPaths); }
  public static MaskingStrategy from(Iterable<String> keys) { return of(keys); }

  public static MaskingStrategy create(Map<String, String> fieldsByKey) { return of(fieldsByKey); }
  public static MaskingStrategy create(Map<String, String> fieldsByKey, List<String> jsonPaths) { return of(fieldsByKey, jsonPaths); }
  public static MaskingStrategy create(Iterable<String> keys) { return of(keys); }

  public static MaskingStrategy build(Map<String, String> fieldsByKey) { return of(fieldsByKey); }
  public static MaskingStrategy build(Map<String, String> fieldsByKey, List<String> jsonPaths) { return of(fieldsByKey, jsonPaths); }
  public static MaskingStrategy build(Iterable<String> keys) { return of(keys); }
  // -------------------------------------------------------------

  @Override
  public Map<String, Object> mask(String entityType, Map<String, Object> before, Map<String, Object> after) {
    Map<String,Object> b = (before == null) ? new HashMap<>() : new HashMap<>(before);
    Map<String,Object> a = (after  == null) ? new HashMap<>() : new HashMap<>(after);
    FieldMasker.maskKeys(b, keys);
    FieldMasker.maskKeys(a, keys);
    Map<String,Object> out = new HashMap<>();
    out.put("before", b);
    out.put("after", a);
    return out;
  }

  private static Set<String> toSet(Iterable<String> it) {
    if (it == null) return Collections.emptySet();
    if (it instanceof Collection<?> c) {
      //noinspection unchecked
      return new LinkedHashSet<>((Collection<String>) c);
    }
    Set<String> s = new LinkedHashSet<>();
    for (String k : it) s.add(k);
    return s;
  }
}
