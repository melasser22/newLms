package com.ejada.template.domain.entity;

import com.ejada.starter_data.tenant.TenantBaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "sendgrid_setting")
public class SendGridSettingEntity extends TenantBaseEntity {

  @Column(nullable = false, length = 128)
  private String secretId;

  @Column(length = 128)
  private String fromEmail;

  @Column(length = 128)
  private String fromName;

  @Column(length = 128)
  private String replyToEmail;

  @Column(nullable = false)
  private boolean sandboxMode;
}
