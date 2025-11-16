package com.ejada.template.exception;

import com.ejada.template.dto.TemplateValidationResponse;
import lombok.Getter;

@Getter
public class TemplateValidationException extends RuntimeException {
  private final TemplateValidationResponse validation;

  public TemplateValidationException(TemplateValidationResponse validation) {
    super("Dynamic data failed validation against allowed variables");
    this.validation = validation;
  }
}
