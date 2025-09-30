package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Product property key/value pairs attached to the subscription.
 */
public record ProductPropertyDto(
        @NotBlank String propertyCd,
        @NotBlank String propertyValue) {
}
