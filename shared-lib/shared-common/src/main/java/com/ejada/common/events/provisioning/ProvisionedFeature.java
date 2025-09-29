package com.ejada.common.events.provisioning;

/** Simple feature entitlement for a provisioned tenant. */
public record ProvisionedFeature(String code, Integer quantity) {
}
