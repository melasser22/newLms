package com.ejada.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Shared enum for integration key status */
@Schema(name = "TikStatus", description = "Integration key status")
public enum TikStatus {
    ACTIVE, SUSPENDED, REVOKED, EXPIRED
}