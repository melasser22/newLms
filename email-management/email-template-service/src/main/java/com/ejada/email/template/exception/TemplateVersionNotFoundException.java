package com.ejada.email.template.exception;

public class TemplateVersionNotFoundException extends RuntimeException {
  public TemplateVersionNotFoundException(Long templateId, Long versionId) {
    super("Template version not found: template=" + templateId + " version=" + versionId);
  }
}
