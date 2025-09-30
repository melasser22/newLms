package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Administrator contact information supplied by the marketplace callbacks.
 */
public record AdminUserInfoDto(
        @NotBlank String adminUserName,
        String preferredLang,
        @NotBlank String mobileNo,
        @Email @NotBlank String email) {
}
