package com.ejada.setup.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemParameterResponse {
    private Integer paramId;
    private String paramKey;
    private String paramValue;
    private String description;
    private String paramGroup;
    private Boolean isActive;
}
