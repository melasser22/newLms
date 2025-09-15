package com.ejada.setup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CountryDto {
  private static final int CODE_LEN = 3;
  private static final int NAME_LEN = 256;
  private static final int DIALING_LEN = 10;
  private static final int DESCRIPTION_LEN = 1000;
  @Null
  private Integer countryId;

  @NotBlank @Size(max = CODE_LEN)
  private String countryCd;

  @NotBlank @Size(max = NAME_LEN)
  private String countryEnNm;

  @NotBlank @Size(max = NAME_LEN)
  private String countryArNm;

  @Size(max = DIALING_LEN)
  private String dialingCode;

  @Size(max = NAME_LEN)
  private String nationalityEn;

  @Size(max = NAME_LEN)
  private String nationalityAr;

  @NotNull
  private Boolean isActive;

  @Size(max = DESCRIPTION_LEN)
  private String enDescription;

  @Size(max = DESCRIPTION_LEN)
  private String arDescription;
}
