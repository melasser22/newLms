package com.ejada.common.sort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.*;

class SortUtilsTest {
    @Test
    void sanitize_invalidPropertyFallsBackToDefault() {
        Sort invalid = Sort.by("unknown");
        Sort sanitized = SortUtils.sanitize(invalid, "name");
        assertEquals("name", sanitized.stream().findFirst().orElseThrow().getProperty());
    }
}
