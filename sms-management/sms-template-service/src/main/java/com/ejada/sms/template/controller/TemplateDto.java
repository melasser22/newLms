package com.ejada.sms.template.controller;

import jakarta.validation.constraints.NotBlank;

public record TemplateDto(
    @NotBlank String code,
    @NotBlank String locale,
    @NotBlank String body,
    String description
) {}
