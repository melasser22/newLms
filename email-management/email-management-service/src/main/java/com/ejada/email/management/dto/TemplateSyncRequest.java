package com.ejada.management.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateSyncRequest(@NotBlank String versionTag) {}
