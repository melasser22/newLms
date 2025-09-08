package com.ejada.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CityDto {
  private static final int CODE_LEN = 50;
  private static final int NAME_LEN = 200;
  @Null // server-generated
  private Integer id;

  @NotBlank @Size(max = CODE_LEN)
  private String cityCd;

  @NotBlank @Size(max = NAME_LEN)
  private String cityEnNm;

  @NotBlank @Size(max = NAME_LEN)
  private String cityArNm;

  @NotNull
  private Boolean isActive;

  @NotNull
  private Integer countryId;
}
