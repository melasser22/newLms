package com.ejada.template.domain.entity;

import com.ejada.starter_data.tenant.TenantBaseEntity;
import com.ejada.template.domain.value.AttachmentMetadata;
import jakarta.persistence.*;
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
@Table(name = "email_template")
public class TemplateEntity extends TenantBaseEntity {

  @Column(nullable = false, length = 128)
  private String name;

  @Column(nullable = false, length = 32)
  private String locale;

  @Column(length = 512)
  private String description;

  @Column(nullable = false)
  private boolean archived = false;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(
      name = "email_template_attachment",
      joinColumns = @JoinColumn(name = "template_id"))
  private Set<AttachmentMetadata> defaultAttachments = new LinkedHashSet<>();

  @OneToMany(
      mappedBy = "template",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private Set<TemplateVersionEntity> versions = new LinkedHashSet<>();
}
