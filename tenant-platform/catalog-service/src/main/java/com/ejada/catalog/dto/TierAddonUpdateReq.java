package com.ejada.catalog.dto;

import java.math.BigDecimal;

public record TierAddonUpdateReq(
    Boolean included,
    Integer sortOrder,
    BigDecimal basePrice,
    String currency,
    Boolean isDeleted
) { }

