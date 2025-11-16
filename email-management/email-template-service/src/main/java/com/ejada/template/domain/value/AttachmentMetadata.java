package com.ejada.template.domain.value;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AttachmentMetadata {

  @Column(name = "attachment_name")
  private String fileName;

  @Column(name = "attachment_type")
  private String contentType;

  @Column(name = "attachment_url", length = 1024)
  private String storageUrl;

  @Column(name = "attachment_size")
  private long sizeInBytes;

  @Column(name = "inline_attachment")
  private boolean inline;
}
