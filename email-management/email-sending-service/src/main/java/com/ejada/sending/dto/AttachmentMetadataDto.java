package com.ejada.sending.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachmentMetadataDto(
    @NotBlank String fileName, @NotBlank String contentType, @NotBlank String url) {}
