package com.ejada.catalog.dto;

public record FeatureUpdateReq(
	    String featureEnNm,
	    String featureArNm,
	    String description,
	    String category,
	    Boolean isMetered,
	    Boolean isActive,
	    Boolean isDeleted
	) {}
