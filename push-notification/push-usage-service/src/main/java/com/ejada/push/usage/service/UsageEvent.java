package com.ejada.push.usage.service;

import jakarta.validation.constraints.NotBlank;

public record UsageEvent(@NotBlank String templateKey, @NotBlank String status) {}
