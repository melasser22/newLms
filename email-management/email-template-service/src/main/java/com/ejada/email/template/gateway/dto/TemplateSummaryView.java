package com.ejada.email.template.gateway.dto;

import java.time.Instant;

public record TemplateSummaryView(Long id, String name, String locale, boolean archived, Instant updatedAt) {}
