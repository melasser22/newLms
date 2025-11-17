package com.ejada.email.template.domain.entity;

import com.ejada.starter_data.tenant.TenantBaseEntity;
import com.ejada.email.template.domain.enums.TemplateVersionStatus;
import com.ejada.email.template.domain.value.AttachmentMetadata;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "email_template_version")
public class TemplateVersionEntity extends TenantBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "template_id", nullable = false)
  private TemplateEntity template;

  @Column(nullable = false)
  private int versionNumber;

  @Column(nullable = false, length = 256)
  private String subject;

  @Lob
  @Column(nullable = false)
  private String htmlBody;

  @Lob
  private String textBody;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "email_template_version_attachment",
      joinColumns = @JoinColumn(name = "template_version_id"))
  private Set<AttachmentMetadata> attachments = new LinkedHashSet<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "email_template_allowed_variable",
      joinColumns = @JoinColumn(name = "template_version_id"))
  @Column(name = "variable_name", length = 128)
  private Set<String> allowedVariables = new LinkedHashSet<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private TemplateVersionStatus status = TemplateVersionStatus.DRAFT;

  private Instant publishedAt;

  @Column(length = 128)
  private String sendGridTemplateId;

  @Column(length = 64)
  private String sendGridVersionId;
}
