package com.ejada.common.sort;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility methods for sanitising {@link Sort} and {@link Pageable} instances.
 * Removes orders with unknown properties and applies a default when necessary
 * to avoid {@code PropertyReferenceException} at repository level.
 */
public final class SortUtils {

    private SortUtils() {
    }

    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Returns a safe {@link Sort}. If the supplied sort is {@code null},
     * unsorted or contains only unknown properties, a sort on
     * {@code defaultProperty} is returned.
     *
     * @param sort            sort to sanitize
     * @param defaultProperty property to use when sort is empty or invalid
     * @param allowedProps    properties allowed for sorting; {@code defaultProperty}
     *                        is always considered allowed
     * @return sanitized Sort never {@code null}
     */
    public static Sort sanitize(Sort sort, String defaultProperty, String... allowedProps) {
        return sanitize(sort, defaultProperty, Sort.Direction.ASC, allowedProps);
    }

    /**
     * Overload allowing the caller to control the default direction used when the
     * incoming sort is {@code null}, empty or contains only disallowed
     * properties. The method is defensive against {@code null} values in
     * {@code allowedProps} and ensures case insensitive comparison of property
     * names so callers do not need to worry about request casing.
     *
     * @param sort             sort to sanitize (can be {@code null})
     * @param defaultProperty  property to use when sort is empty or invalid
     * @param defaultDirection direction to apply for the default property
     * @param allowedProps     properties allowed for sorting; {@code null} entries
     *                         are ignored
     * @return sanitized Sort never {@code null}
     */
    public static Sort sanitize(Sort sort,
                                String defaultProperty,
                                Sort.Direction defaultDirection,
                                String... allowedProps) {
        Objects.requireNonNull(defaultProperty, "defaultProperty must not be null");
        Objects.requireNonNull(defaultDirection, "defaultDirection must not be null");

        Sort safeSort = sort == null ? Sort.unsorted() : sort;
        Set<String> allowed = new HashSet<>();
        if (allowedProps != null) {
            Arrays.stream(allowedProps)
                    .filter(prop -> prop != null && !prop.isBlank())
                    .map(prop -> prop.toLowerCase(Locale.ROOT))
                    .forEach(allowed::add);
        }
        String defaultKey = defaultProperty.toLowerCase(Locale.ROOT);
        allowed.add(defaultKey);

        Map<String, Sort.Order> orders = new LinkedHashMap<>();
        safeSort.stream()
                .filter(order -> order != null && order.getProperty() != null && !order.getProperty().isBlank())
                .forEach(order -> {
                    String key = order.getProperty().toLowerCase(Locale.ROOT);
                    if (allowed.contains(key)) {
                        orders.putIfAbsent(key, order);
                    }
                });
        if (orders.isEmpty()) {
            return Sort.by(new Sort.Order(defaultDirection, defaultProperty));
        }
        return Sort.by(new ArrayList<>(orders.values()));
    }

    /**
     * Creates a {@link Pageable} with sanitized sort. Page number and size are
     * preserved from the given pageable.
     *
     * @param pageable        pageable to sanitize (can be {@code null})
     * @param defaultProperty property to use when sort is empty or invalid
     * @param allowedProps    properties allowed for sorting
     * @return sanitized pageable
     */
    public static Pageable sanitize(Pageable pageable, String defaultProperty, String... allowedProps) {
        return sanitize(pageable, defaultProperty, Sort.Direction.ASC, DEFAULT_PAGE_SIZE, allowedProps);
    }

    /**
     * Variant of {@link #sanitize(Pageable, String, String...)} that additionally
     * lets the caller control the default direction and the default page size to
     * use when the incoming pageable is {@code null} or unpaged.
     *
     * @param pageable         pageable to sanitize (can be {@code null})
     * @param defaultProperty  property to use when sort is empty or invalid
     * @param defaultDirection direction to apply for the default property
     * @param defaultPageSize  page size to use when pageable is {@code null} or has
     *                         a non-positive size
     * @param allowedProps     properties allowed for sorting
     * @return sanitized pageable
     */
    public static Pageable sanitize(Pageable pageable,
                                     String defaultProperty,
                                     Sort.Direction defaultDirection,
                                     int defaultPageSize,
                                     String... allowedProps) {
        Objects.requireNonNull(defaultProperty, "defaultProperty must not be null");
        Objects.requireNonNull(defaultDirection, "defaultDirection must not be null");
        int safePageSize = defaultPageSize > 0 ? defaultPageSize : DEFAULT_PAGE_SIZE;

        if (pageable == null || pageable.isUnpaged()) {
            Sort sort = sanitize(Sort.unsorted(), defaultProperty, defaultDirection, allowedProps);
            return PageRequest.of(0, safePageSize, sort);
        }
        Sort sort = sanitize(pageable.getSort(), defaultProperty, defaultDirection, allowedProps);
        int size = pageable.getPageSize() > 0 ? pageable.getPageSize() : safePageSize;
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
