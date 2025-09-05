package com.ejada.catalog.dto;

public record AddonUpdateReq(
	    String addonEnNm,
	    String addonArNm,
	    String description,
	    String category,
	    Boolean isActive,
	    Boolean isDeleted
	) {}