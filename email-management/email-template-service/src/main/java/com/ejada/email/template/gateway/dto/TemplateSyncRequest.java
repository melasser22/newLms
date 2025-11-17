package com.ejada.template.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateSyncRequest(@NotBlank String versionTag) {}
