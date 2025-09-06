package com.ejada.catalog.dto;

import java.math.BigDecimal;

public record TierAddonRes(
	    Integer tierAddonId,
	    Integer tierId,
	    Integer addonId,
	    Boolean included,
	    Integer sortOrder,
	    BigDecimal basePrice,
	    String currency,
	    Boolean isDeleted
	) {}