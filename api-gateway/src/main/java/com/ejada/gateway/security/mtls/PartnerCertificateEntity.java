package com.ejada.gateway.security.mtls;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(value = "gateway_partner_certificates", schema = "public")
public class PartnerCertificateEntity {

  @Id
  private Long id;

  @Column("tenant_id")
  private String tenantId;

  @Column("fingerprint_sha256")
  private String fingerprintSha256;

  @Column("subject_dn")
  private String subjectDn;

  @Column("valid_from")
  private Instant validFrom;

  @Column("valid_to")
  private Instant validTo;

  private boolean revoked;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public String getFingerprintSha256() {
    return fingerprintSha256;
  }

  public void setFingerprintSha256(String fingerprintSha256) {
    this.fingerprintSha256 = fingerprintSha256;
  }

  public String getSubjectDn() {
    return subjectDn;
  }

  public void setSubjectDn(String subjectDn) {
    this.subjectDn = subjectDn;
  }

  public Instant getValidFrom() {
    return validFrom;
  }

  public void setValidFrom(Instant validFrom) {
    this.validFrom = validFrom;
  }

  public Instant getValidTo() {
    return validTo;
  }

  public void setValidTo(Instant validTo) {
    this.validTo = validTo;
  }

  public boolean isRevoked() {
    return revoked;
  }

  public void setRevoked(boolean revoked) {
    this.revoked = revoked;
  }
}
