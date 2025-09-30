package com.ejada.common.marketplace.dto;

/**
 * Identifier values we may return to the marketplace after provisioning completes.
 */
public record EnvironmentIdentifierDto(
        String identifierCd,
        String identifierValue) {
}
