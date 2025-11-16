package com.ejada.template.domain.entity;

import com.ejada.starter_data.tenant.TenantBaseEntity;
import com.ejada.template.domain.enums.EmailSendMode;
import com.ejada.template.domain.enums.EmailSendStatus;
import com.ejada.template.domain.value.AttachmentMetadata;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "email_send")
public class EmailSendEntity extends TenantBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "template_version_id")
  private TemplateVersionEntity templateVersion;

  @ElementCollection
  @CollectionTable(name = "email_send_recipient", joinColumns = @JoinColumn(name = "send_id"))
  @Column(name = "recipient")
  private List<String> recipients;

  @ElementCollection
  @CollectionTable(name = "email_send_cc", joinColumns = @JoinColumn(name = "send_id"))
  @Column(name = "cc")
  private List<String> cc;

  @ElementCollection
  @CollectionTable(name = "email_send_bcc", joinColumns = @JoinColumn(name = "send_id"))
  @Column(name = "bcc")
  private List<String> bcc;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> dynamicData;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "email_send_attachment", joinColumns = @JoinColumn(name = "send_id"))
  private Set<AttachmentMetadata> attachments = new LinkedHashSet<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EmailSendStatus status = EmailSendStatus.QUEUED;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EmailSendMode mode = EmailSendMode.PRODUCTION;

  @Column(length = 128, unique = true)
  private String idempotencyKey;

  @Column(length = 128)
  private String sendGridMessageId;

  private Instant requestedAt;

  private Instant processedAt;

  @Column(length = 128)
  private String errorCode;

  @Column(length = 512)
  private String errorMessage;
}
