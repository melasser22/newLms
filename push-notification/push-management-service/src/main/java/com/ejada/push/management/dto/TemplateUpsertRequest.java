package com.ejada.push.management.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record TemplateUpsertRequest(
    @NotBlank String key,
    @NotBlank String locale,
    @NotBlank String title,
    @NotBlank String body,
    String imageUrl,
    Map<String, String> data) {}
