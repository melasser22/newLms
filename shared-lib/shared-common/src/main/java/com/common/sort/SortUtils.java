package com.common.sort;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
        Sort safeSort = sort == null ? Sort.unsorted() : sort;
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedProps));
        allowed.add(defaultProperty);
        List<Sort.Order> orders = new ArrayList<>();
        safeSort.stream()
                .filter(order -> allowed.isEmpty() || allowed.contains(order.getProperty()))
                .forEach(orders::add);
        if (orders.isEmpty()) {
            return Sort.by(defaultProperty).ascending();
        }
        return Sort.by(orders);
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
        if (pageable == null || pageable.isUnpaged()) {
            Sort sort = sanitize(Sort.unsorted(), defaultProperty, allowedProps);
            return PageRequest.of(0, DEFAULT_PAGE_SIZE, sort);
        }
        Sort sort = sanitize(pageable.getSort(), defaultProperty, allowedProps);
        int size = pageable.getPageSize() > 0 ? pageable.getPageSize() : DEFAULT_PAGE_SIZE;
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
