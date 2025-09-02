package com.ejada.starter_security.web;

final class WebUtils {
  private WebUtils() {}

  static String safe(String s, String fallback) {
    return (s == null || s.isBlank()) ? fallback : s;
  }

  static String firstNonBlank(String... values) {
    if (values == null) return null;
    for (String v : values) {
      if (v != null && !v.isBlank()) {
        return v;
      }
    }
    return null;
  }
}
