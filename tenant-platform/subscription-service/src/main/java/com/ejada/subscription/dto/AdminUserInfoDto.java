package com.ejada.subscription.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AdminUserInfoDto(
    @NotBlank String adminUserName,
    String preferredLang,    // EN | AR
    @NotBlank String mobileNo,
    @Email @NotBlank String email
) { }
