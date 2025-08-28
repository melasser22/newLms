package com.common.masking;

import java.util.regex.Pattern;

/**
 * Utility for masking sensitive information before logging or exposing in
 * responses.
 * Centralizing this ensures consistent handling across all services.
 */
public final class MaskingUtil {

    private MaskingUtil() {
        // utility class
    }

    // ========= Common Masks =========

    /** Mask for email addresses */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(?<=[\\w]{2})[\\w._%+-]*(@[\\w.-]+)");

    /** Mask for credit card numbers (keep last 4 digits) */
    private static final Pattern CARD_PATTERN = Pattern.compile("\\b(\\d{6})\\d{6,9}(\\d{4})\\b");

    /** Mask for Saudi/Iqama/National IDs (show first 2 and last 2 digits) */
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("\\b(\\d{2})\\d{4,8}(\\d{2})\\b");

    // ========= Public API =========

    /** Mask email, leaving first 2 chars and domain */
    public static String maskEmail(String value) {
        if (value == null)
            return null;
        return EMAIL_PATTERN.matcher(value).replaceAll("****$1");
    }

    /** Mask credit/debit card, keep BIN and last 4 digits */
    public static String maskCard(String value) {
        if (value == null)
            return null;
        return CARD_PATTERN.matcher(value).replaceAll("$1******$2");
    }

    /** Mask National ID, leave first 2 and last 2 digits */
    public static String maskNationalId(String value) {
        if (value == null)
            return null;
        return NATIONAL_ID_PATTERN.matcher(value).replaceAll("$1******$2");
    }

    /** Generic mask: replace all but first and last char with * */
    public static String maskGeneric(String value) {
        if (value == null || value.length() <= 2)
            return value;
        return value.charAt(0) + "*".repeat(value.length() - 2) + value.charAt(value.length() - 1);
    }

    /** Mask passwords completely */
    public static String maskPassword(String value) {
        return value == null ? null : "****";
    }
}
