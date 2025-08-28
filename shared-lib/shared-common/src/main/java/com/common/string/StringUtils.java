package com.common.string;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Centralized string utilities for Shared.
 * Focus on safety, null-handling, and common transformations.
 */
public final class StringUtils {

    private StringUtils() {
    }

    // ==== Must-have basics ====

    /** Check if string is null or empty */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /** Check if string is null, empty, or only whitespace */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /** Safe trim: returns null if input is null */
    public static String trimToNull(String str) {
        if (str == null)
            return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Default if null or empty */
    public static String defaultIfEmpty(String str, String defaultVal) {
        return isEmpty(str) ? defaultVal : str;
    }

    // ==== Should-have helpers ====

    /** Convert to upper safely */
    public static String toUpperSafe(String str) {
        return str == null ? null : str.toUpperCase();
    }

    /** Convert to lower safely */
    public static String toLowerSafe(String str) {
        return str == null ? null : str.toLowerCase();
    }

    /** Repeat a string n times */
    public static String repeat(String str, int times) {
        if (isEmpty(str) || times <= 0)
            return "";
        return str.repeat(times);
    }

    /** Abbreviate with ellipsis */
    public static String abbreviate(String str, int maxLen) {
        if (str == null)
            return null;
        if (str.length() <= maxLen)
            return str;
        return str.substring(0, Math.max(0, maxLen - 3)) + "...";
    }

    // ==== Nice-to-have utilities ====

    /** Generate random UUID string */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /** Base64 encode */
    public static String base64Encode(String str) {
        if (str == null)
            return null;
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    /** Base64 decode (safe) */
    public static Optional<String> base64DecodeSafe(String str) {
        try {
            byte[] decoded = Base64.getDecoder().decode(str);
            return Optional.of(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Return the given string if non-null, otherwise return an empty string.
     *
     * <p>This helper is useful to safely print values that may be null without
     * introducing "null" into logs or responses. It does not trim the
     * input.</p>
     *
     * @param str input string
     * @return the input string or an empty string if {@code str} is null
     */
    public static String safe(String str) {
        return str == null ? "" : str;
    }

    /**
     * Return the first non-blank string from the provided values or {@code null}
     * if none are non-blank.
     *
     * <p>This method iterates through the provided values and returns the
     * first value that is not {@code null} and not blank according to
     * {@link String#isBlank()}. Unlike {@link #defaultIfEmpty}, this helper
     * does not substitute a default but instead selects among multiple
     * candidate values.</p>
     *
     * @param values candidate strings
     * @return the first non-blank value or {@code null} if none
     */
    public static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    /** Mask sensitive string (show first/last N chars) */
    public static String mask(String str, int visibleStart, int visibleEnd) {
        if (isEmpty(str))
            return str;
        int length = str.length();
        if (length <= visibleStart + visibleEnd)
            return str; // nothing to mask
        StringBuilder sb = new StringBuilder();
        sb.append(str, 0, visibleStart);
        sb.append("*".repeat(length - (visibleStart + visibleEnd)));
        sb.append(str.substring(length - visibleEnd));
        return sb.toString();
    }

}
