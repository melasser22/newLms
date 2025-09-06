package com.ejada.catalog.dto;
import java.math.BigDecimal;
import com.ejada.catalog.model.Enforcement;
import com.ejada.catalog.model.LimitWindow;
import com.ejada.catalog.model.MeasureUnit;

public record AddonFeatureCreateReq(
	    Integer addonId,
	    Integer featureId,
	    Boolean enabled,
	    Enforcement enforcement,
	    BigDecimal softLimit,
	    BigDecimal hardLimit,
	    LimitWindow limitWindow,
	    MeasureUnit measureUnit,
	    String resetCron,
	    Boolean overageEnabled,
	    BigDecimal overageUnitPrice,
	    String overageCurrency,
	    String meta
	) {}