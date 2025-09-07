package com.ejada.subscription.dto;

import jakarta.validation.constraints.*;

public record CustomerInfoDto(
    String customerNameEn,
    String customerNameAr,
    String customerType,      // e.g., COMPANY
    String crNumber,
    String countryCd,
    String cityCd,
    String addressEn,
    String addressAr,
    @Email String email,
    String mobileNo
) {}
