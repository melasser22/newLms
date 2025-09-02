package com.ejada.setup.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CityDto {
  @Null // server-generated
  private Integer id;

  @NotBlank @Size(max = 50)
  private String cityCd;

  @NotBlank @Size(max = 200)
  private String cityEnNm;

  @NotBlank @Size(max = 200)
  private String cityArNm;

  @NotNull
  private Boolean isActive;

  @NotNull
  private Integer countryId;
}
