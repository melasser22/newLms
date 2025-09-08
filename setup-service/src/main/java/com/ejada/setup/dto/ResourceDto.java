package com.ejada.setup.dto;

import com.ejada.setup.constants.ValidationConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResourceDto {
    @Null
    private Integer id;

    @NotBlank
    @Size(max = ValidationConstants.CODE_LEN)
    private String resourceCd;

    @NotBlank
    @Size(max = ValidationConstants.NAME_LEN)
    private String resourceEnNm;

    @NotBlank
    @Size(max = ValidationConstants.NAME_LEN)
    private String resourceArNm;

    @Size(max = ValidationConstants.PATH_LEN)
    private String path;

    @Size(max = ValidationConstants.HTTP_METHOD_LEN)
    private String httpMethod;

    private Integer parentResourceId;

    @NotNull
    private Boolean isActive;

    @Size(max = ValidationConstants.TEXT_LEN_1000)
    private String enDescription;

    @Size(max = ValidationConstants.TEXT_LEN_1000)
    private String arDescription;
}
