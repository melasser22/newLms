package com.ejada.email.template.domain.entity;

import com.ejada.starter_data.tenant.TenantBaseEntity;
import com.ejada.email.template.domain.enums.EmailEventType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "email_event")
public class EmailEventEntity extends TenantBaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "email_send_id")
  private EmailSendEntity emailSend;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private EmailEventType eventType;

  private Instant eventTimestamp;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> payload;

  @Column(length = 128)
  private String sendGridMessageId;
}
