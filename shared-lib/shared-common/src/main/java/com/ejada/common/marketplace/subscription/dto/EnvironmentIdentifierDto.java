package com.ejada.common.marketplace.subscription.dto;

/** Identifier values we may return to marketplace after provisioning. */
public record EnvironmentIdentifierDto(
        String identifierCd,
        String identifierValue) {
}
