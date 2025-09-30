package com.ejada.common.marketplace.subscription.dto;

import jakarta.validation.constraints.Email;

public record CustomerInfoDto(
        String customerNameEn,
        String customerNameAr,
        String customerType,
        String crNumber,
        String countryCd,
        String cityCd,
        String addressEn,
        String addressAr,
        @Email String email,
        String mobileNo) {
}
