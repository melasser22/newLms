package com.ejada.subscription.dto;

import jakarta.validation.constraints.NotBlank;

public record ProductPropertyDto(
    @NotBlank String propertyCd,
    @NotBlank String propertyValue
) { }