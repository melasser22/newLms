package com.ejada.gateway.routes.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a predicate or filter definition as stored in the database. Each component has a
 * Spring Cloud Gateway style {@code name} and a map of arguments.
 */
public record RouteComponent(String name, Map<String, String> args) {

  public RouteComponent {
    String trimmed = (name == null) ? null : name.trim();
    if (trimmed == null || trimmed.isEmpty()) {
      throw new IllegalArgumentException("Component name must not be blank");
    }
    name = trimmed;

    Map<String, String> safeArgs = new LinkedHashMap<>();
    if (args != null) {
      args.forEach((key, value) -> {
        if (key != null && !key.isBlank() && value != null) {
          safeArgs.put(key.trim(), value.trim());
        }
      });
    }
    args = Collections.unmodifiableMap(safeArgs);
  }

  public RouteComponent mergeArgs(Map<String, String> extraArgs) {
    if (extraArgs == null || extraArgs.isEmpty()) {
      return this;
    }
    Map<String, String> merged = new LinkedHashMap<>(args);
    extraArgs.forEach((key, value) -> {
      if (key != null && value != null) {
        merged.put(key.trim(), value.trim());
      }
    });
    return new RouteComponent(name, merged);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RouteComponent that)) {
      return false;
    }
    return name.equalsIgnoreCase(that.name) && Objects.equals(args, that.args);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name.toLowerCase(), args);
  }
}
