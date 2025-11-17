package com.ejada.push.device.service;

import jakarta.validation.constraints.NotBlank;

public record DeviceRegistrationRequest(
    @NotBlank String userId,
    @NotBlank String deviceToken,
    @NotBlank String platform,
    String appId) {}
