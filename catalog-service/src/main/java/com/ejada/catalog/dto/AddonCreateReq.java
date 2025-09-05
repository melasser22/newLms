package com.ejada.catalog.dto;

public record AddonCreateReq(
	    String addonCd,
	    String addonEnNm,
	    String addonArNm,
	    String description,
	    String category,
	    Boolean isActive
	) {}