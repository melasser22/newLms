package com.ejada.common.marketplace.dto;

import jakarta.validation.constraints.Email;

/**
 * Customer profile metadata included in marketplace subscription payloads.
 */
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
