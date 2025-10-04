package com.ejada.gateway.versioning;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.util.StringUtils;

/**
 * Utility methods for dealing with API version tokens. Versions are parsed as
 * dot separated positive integers (e.g. {@code v1}, {@code 2.0},
 * {@code V2.1.3}). The canonical representation always uses a {@code v}
 * prefix, removes redundant leading zeroes and trims trailing zero components
 * (so {@code v1.0.0} normalises to {@code v1}).
 */
public final class VersionNumber {

  private static final Pattern NUMERIC_PATTERN = Pattern.compile("^(\\d+)(?:\\.\\d+)*$");

  private VersionNumber() {
  }

  /**
   * Returns the canonical representation of the supplied version token.
   *
   * @param value raw version token (may optionally start with {@code v})
   * @return canonical version token with a {@code v} prefix
   * @throws IllegalArgumentException if the supplied value is blank or does not
   *                                  follow the numeric version format
   */
  public static String canonicalise(String value) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException("Version token must not be blank");
    }

    String candidate = value.trim().toLowerCase(Locale.ROOT);
    if (candidate.startsWith("v")) {
      candidate = candidate.substring(1);
    }

    if (!NUMERIC_PATTERN.matcher(candidate).matches()) {
      throw new IllegalArgumentException("Version token must be numeric (e.g. v1, 2.0)");
    }

    String[] components = candidate.split("\\.");
    List<String> normalised = new ArrayList<>(components.length);
    for (String component : components) {
      normalised.add(normaliseComponent(component));
    }

    int lastNonZero = normalised.size() - 1;
    while (lastNonZero > 0 && "0".equals(normalised.get(lastNonZero))) {
      lastNonZero--;
    }

    StringBuilder builder = new StringBuilder("v");
    for (int i = 0; i <= lastNonZero; i++) {
      builder.append(normalised.get(i));
      if (i < lastNonZero) {
        builder.append('.');
      }
    }
    return builder.toString();
  }

  private static String normaliseComponent(String component) {
    BigInteger numeric = new BigInteger(component);
    return numeric.toString();
  }

  /**
   * Attempts to canonicalise the supplied value. Returns {@code null} when the
   * value is blank or not a valid version.
   */
  public static String canonicaliseOrNull(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return canonicalise(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
