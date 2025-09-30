package com.ejada.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemParameterRequest {

    private static final int KEY_LENGTH = 150;
    private static final int VALUE_LENGTH = 1000;

    @NotBlank
    @Size(max = KEY_LENGTH)
    private String paramKey;

    @NotBlank
    @Size(max = VALUE_LENGTH)
    private String paramValue;

    @Size(max = VALUE_LENGTH)
    private String description;

    @Size(max = KEY_LENGTH)
    private String paramGroup;

    @NotNull
    private Boolean isActive;
}
