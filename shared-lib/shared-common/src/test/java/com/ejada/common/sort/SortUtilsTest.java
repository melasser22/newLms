package com.ejada.common.sort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortUtilsTest {
    @Test
    void sanitize_invalidPropertyFallsBackToDefault() {
        Sort invalid = Sort.by("unknown");
        Sort sanitized = SortUtils.sanitize(invalid, "name");
        assertEquals("name", sanitized.stream().findFirst().orElseThrow().getProperty());
    }

    @Test
    void sanitize_respectsDefaultDirectionWhenFallbackNeeded() {
        Sort invalid = Sort.by("unknown");
        Sort sanitized = SortUtils.sanitize(invalid, "createdAt", Sort.Direction.DESC, "name");

        Sort.Order order = sanitized.stream().findFirst().orElseThrow();
        assertEquals("createdAt", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void sanitize_ignoresCaseAndDuplicateProperties() {
        Sort raw = Sort.by(Sort.Order.desc("Name"), Sort.Order.asc("NAME"), Sort.Order.asc("other"));

        Sort sanitized = SortUtils.sanitize(raw, "createdAt", Sort.Direction.ASC, "name");
        List<Sort.Order> orders = sanitized.stream().toList();

        assertEquals(1, orders.size(), "Duplicate properties should be collapsed");
        Sort.Order order = orders.get(0);
        assertEquals("Name", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection(), "Original direction should be preserved");
    }

    @Test
    void sanitize_allowsNullAllowedProps() {
        Sort sanitized = SortUtils.sanitize(null, "createdAt", Sort.Direction.ASC, (String[]) null);
        Sort.Order order = sanitized.stream().findFirst().orElseThrow();

        assertEquals("createdAt", order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void sanitizePageable_unpagedUsesDefaults() {
        Pageable sanitized = SortUtils.sanitize(Pageable.unpaged(), "createdAt", Sort.Direction.DESC, 50, "createdAt");

        assertEquals(0, sanitized.getPageNumber());
        assertEquals(50, sanitized.getPageSize());
        Sort.Order order = sanitized.getSort().stream().findFirst().orElseThrow();
        assertEquals("createdAt", order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void sanitizePageable_preservesPageRequestValues() {
        Pageable pageable = PageRequest.of(2, 10, Sort.by(Sort.Order.desc("name"), Sort.Order.asc("other")));

        Pageable sanitized = SortUtils.sanitize(pageable, "createdAt", Sort.Direction.ASC, 25, "name");

        assertEquals(2, sanitized.getPageNumber());
        assertEquals(10, sanitized.getPageSize());
        assertTrue(sanitized.getSort().getOrderFor("name").isDescending());
        assertEquals(1, sanitized.getSort().stream().count(), "Only allowed property should remain");
    }
}
