package com.common.time;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Centralized date/time utility methods.
 * 
 * Uses java.time (Java 8+) APIs instead of legacy Date/Calendar.
 */
public final class DateUtils {

    private DateUtils() {
    }

    // ==== Formatters ====
    public static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;
    public static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;
    public static final DateTimeFormatter HUMAN_READABLE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==== Now / Today ====
    public static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static Instant instantNow() {
        return Instant.now();
    }

    // ==== Conversion ====
    public static Date toDate(Instant instant) {
        return Date.from(instant);
    }

    public static Instant toInstant(Date date) {
        return date.toInstant();
    }

    public static LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static Date toDate(LocalDateTime ldt) {
        return Date.from(ldt.toInstant(ZoneOffset.UTC));
    }

    // ==== Formatting ====
    public static String format(LocalDateTime ldt) {
        return ldt.format(HUMAN_READABLE);
    }

    public static String format(LocalDateTime ldt, DateTimeFormatter formatter) {
        return ldt.format(formatter);
    }

    public static Optional<LocalDateTime> parseSafe(String text, DateTimeFormatter formatter) {
        try {
            return Optional.of(LocalDateTime.parse(text, formatter));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // ==== Utility Methods ====
    public static boolean isBeforeNow(Instant instant) {
        return instant.isBefore(Instant.now());
    }

    public static boolean isAfterNow(Instant instant) {
        return instant.isAfter(Instant.now());
    }

    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime.plusDays(days);
    }

    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        return dateTime.minusDays(days);
    }
}
