package com.ejada.setup.dto;

import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CountryDto {
  @Null
  private Integer countryId;

  @NotBlank @Size(max = 3)
  private String countryCd;

  @NotBlank @Size(max = 256)
  private String countryEnNm;

  @NotBlank @Size(max = 256)
  private String countryArNm;

  @Size(max = 10)
  private String dialingCode;

  @Size(max = 256)
  private String nationalityEn;

  @Size(max = 256)
  private String nationalityAr;

  @NotNull
  private Boolean isActive;

  @Size(max = 1000)
  private String enDescription;

  @Size(max = 1000)
  private String arDescription;
}
