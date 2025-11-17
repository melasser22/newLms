package com.ejada.sending.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_logs")
public class EmailLog {

  @Id
  private String id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "template_key", nullable = false)
  private String templateKey;

  @Column(name = "mode", nullable = false)
  private String mode;

  @Column(name = "to_recipients", length = 2000)
  private String toRecipients;

  @Column(name = "cc_recipients", length = 2000)
  private String ccRecipients;

  @Column(name = "bcc_recipients", length = 2000)
  private String bccRecipients;

  @Enumerated(EnumType.STRING)
  private EmailStatus status;

  @Column(name = "attempt_count")
  private int attemptCount;

  @Column(name = "idempotency_key")
  private String idempotencyKey;

  @Column(name = "last_error", length = 4000)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public void setTemplateKey(String templateKey) {
    this.templateKey = templateKey;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = mode;
  }

  public String getToRecipients() {
    return toRecipients;
  }

  public void setToRecipients(String toRecipients) {
    this.toRecipients = toRecipients;
  }

  public String getCcRecipients() {
    return ccRecipients;
  }

  public void setCcRecipients(String ccRecipients) {
    this.ccRecipients = ccRecipients;
  }

  public String getBccRecipients() {
    return bccRecipients;
  }

  public void setBccRecipients(String bccRecipients) {
    this.bccRecipients = bccRecipients;
  }

  public EmailStatus getStatus() {
    return status;
  }

  public void setStatus(EmailStatus status) {
    this.status = status;
  }

  public int getAttemptCount() {
    return attemptCount;
  }

  public void setAttemptCount(int attemptCount) {
    this.attemptCount = attemptCount;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getLastError() {
    return lastError;
  }

  public void setLastError(String lastError) {
    this.lastError = lastError;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  @PrePersist
  public void onCreate() {
    Instant now = Instant.now();
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  public void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
