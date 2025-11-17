package com.ejada.email.management.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateSyncRequest(@NotBlank String versionTag) {}
