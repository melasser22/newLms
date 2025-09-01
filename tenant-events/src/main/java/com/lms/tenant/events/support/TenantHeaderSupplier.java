package com.lms.tenant.events.support;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/** Supplies cross-cutting headers (tenant_id, trace_id). Default uses MDC + optional TenantContext if present. */
public interface TenantHeaderSupplier {
  Map<String, Object> headers();

  class Default implements TenantHeaderSupplier {
    @Override public Map<String, Object> headers() {
      Map<String,Object> h = new HashMap<>();
      String tid = MDC.get("tenant_id");
      if (tid == null) {
        try {
          Class<?> ctx = Class.forName("com.lms.tenant.config.TenantContext");
          String val = (String) ctx.getMethod("get").invoke(null);
          if (val != null) tid = val;
        } catch (Throwable ignored) {}
      }
      if (tid != null) h.put("tenant_id", tid);
      String trace = MDC.get("trace_id");
      if (trace != null) h.put("trace_id", trace);
      return h;
    }
  }
}
