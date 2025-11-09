package com.ejada.subscription.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(name = "GetSubscriptionTokenRq", description = "Subscription token request payload")
public record GetSubscriptionTokenRq(
    @NotBlank
    @Schema(description = "Unique user login name", example = "xyz")
    String loginName,

    @NotBlank
    @Pattern(regexp = "^[A-Fa-f0-9]{64}$", message = "password must be a 64 character SHA-256 hex value")
    @Schema(
        description = "User password hashed with SHA-256 and encoded as hex",
        example = "b03ddf3ca2e714a6548e7495e2a03f5e824eaac9837cd7f159c67b90fb4b7342"
    )
    String password
) { }
