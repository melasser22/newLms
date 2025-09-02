package com.ejada.setup.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceDto {
    @Null
    private Integer id;

    @NotBlank
    @Size(max = 128)
    private String resourceCd;

    @NotBlank
    @Size(max = 256)
    private String resourceEnNm;

    @NotBlank
    @Size(max = 256)
    private String resourceArNm;

    @Size(max = 512)
    private String path;

    @Size(max = 16)
    private String httpMethod;

    private Integer parentResourceId;

    @NotNull
    private Boolean isActive;

    @Size(max = 1000)
    private String enDescription;

    @Size(max = 1000)
    private String arDescription;
}
