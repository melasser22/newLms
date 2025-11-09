package com.ejada.subscription.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "GetSubscriptionTokenRs", description = "Subscription token response payload")
public record GetSubscriptionTokenRs(
    @Schema(description = "Signed JWT token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token
) { }
