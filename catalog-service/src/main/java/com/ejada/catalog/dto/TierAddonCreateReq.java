package com.ejada.catalog.dto;

import java.math.BigDecimal;

public record TierAddonCreateReq(
	    Integer tierId,
	    Integer addonId,
	    Boolean included,
	    Integer sortOrder,
	    BigDecimal basePrice,
	    String currency
	) {}