package com.ejada.push.template.service;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record TemplateRequest(
    @NotBlank String key,
    @NotBlank String locale,
    @NotBlank String title,
    @NotBlank String body,
    String imageUrl,
    Map<String, String> data) {}
