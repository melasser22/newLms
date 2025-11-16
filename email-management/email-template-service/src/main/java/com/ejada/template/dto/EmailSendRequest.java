package com.ejada.template.dto;

import com.ejada.template.domain.enums.EmailSendMode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class EmailSendRequest {
  @NotNull private Long templateId;
  private Long templateVersionId;

  @NotEmpty private List<@Email String> recipients;

  private List<@Email String> cc;
  private List<@Email String> bcc;

  private Map<String, Object> dynamicData;
  private List<AttachmentMetadataDto> attachments;

  @Size(max = 128)
  private String idempotencyKey;

  private EmailSendMode mode = EmailSendMode.PRODUCTION;
  private Map<String, Object> customArgs;

  private String subjectOverride;
}
