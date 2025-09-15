package com.ejada.subscription.dto;


/** Identifier values we may return to marketplace after provisioning. */
public record EnvironmentIdentifierDto(
     String identifierCd,     // e.g., DB_ID
     String identifierValue   // e.g., "10.20.0.1"
) { }