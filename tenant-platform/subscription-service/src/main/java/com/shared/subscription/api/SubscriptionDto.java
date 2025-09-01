package com.shared.subscription.api;

import java.util.UUID;

public record SubscriptionDto(UUID id, UUID tenantId, String status) {}
