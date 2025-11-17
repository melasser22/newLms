package com.ejada.email.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AttachmentMetadataDto {
  @NotBlank String fileName;
  @NotBlank String contentType;
  @NotBlank String storageUrl;
  long sizeInBytes;
  @NotNull Boolean inline;
}
