package com.ejada.email.management.dto;

import java.time.Instant;
public record TemplateSummary(Long id, String name, String locale, boolean archived, Instant updatedAt) {}
