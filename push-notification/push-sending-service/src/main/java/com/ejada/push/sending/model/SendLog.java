package com.ejada.push.sending.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SendLog {
  private final String requestId;
  private final String tenantId;
  private final List<String> tokens;
  private final String templateKey;
  private final String status;
  private final Instant createdAt;
  private final Map<String, String> variables;

  public SendLog(
      String requestId,
      String tenantId,
      List<String> tokens,
      String templateKey,
      Map<String, String> variables,
      String status) {
    this.requestId = requestId;
    this.tenantId = tenantId;
    this.tokens = tokens;
    this.templateKey = templateKey;
    this.variables = variables;
    this.status = status;
    this.createdAt = Instant.now();
  }

  public String getRequestId() {
    return requestId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public List<String> getTokens() {
    return tokens;
  }

  public String getTemplateKey() {
    return templateKey;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Map<String, String> getVariables() {
    return variables;
  }
}
