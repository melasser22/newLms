package com.ejada.catalog.dto;

public record AddonRes(
	    Integer addonId,
	    String addonCd,
	    String addonEnNm,
	    String addonArNm,
	    String description,
	    String category,
	    Boolean isActive,
	    Boolean isDeleted
	) {}