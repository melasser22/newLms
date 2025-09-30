package com.ejada.common.marketplace.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminUserInfoDto(
        @NotBlank String adminUserName,
        String preferredLang,
        @NotBlank String mobileNo,
        @Email @NotBlank String email) {
}
