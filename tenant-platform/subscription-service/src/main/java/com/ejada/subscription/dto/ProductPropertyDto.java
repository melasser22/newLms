package com.ejada.subscription.dto;

import jakarta.validation.constraints.*;

public record ProductPropertyDto(
    @NotBlank String propertyCd,
    @NotBlank String propertyValue
) {}