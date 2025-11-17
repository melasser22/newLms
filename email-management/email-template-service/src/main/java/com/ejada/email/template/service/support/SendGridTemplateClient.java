package com.ejada.email.template.service.support;

import com.ejada.email.template.config.SendGridProperties;
import com.ejada.email.template.domain.entity.TemplateEntity;
import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.exception.SendGridSyncException;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendGridTemplateClient {

  private final SendGrid sendGrid;
  private final SendGridProperties properties;

  public void syncTemplate(TemplateEntity template, TemplateVersionEntity version) {
    try {
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint("/v3/templates");
      request.setBody(
          "{" +
              "\"name\":\"" + template.getName() + " v" + version.getVersionNumber() + "\"," +
              "\"generation\":\"dynamic\"}");
      Response response = sendGrid.api(request);
      if (response.getStatusCode() >= 400) {
        throw new SendGridSyncException("Failed to sync template: " + response.getBody());
      }
      version.setSendGridTemplateId(template.getName());
      version.setSendGridVersionId("v" + version.getVersionNumber());
      log.info("SendGrid template sync succeeded for template {}", template.getName());
    } catch (IOException ex) {
      throw new SendGridSyncException("SendGrid API failure", ex);
    }
  }

  public void publishVersion(TemplateEntity template, TemplateVersionEntity version) {
    try {
      String templateId =
          version.getSendGridTemplateId() != null
              ? version.getSendGridTemplateId()
              : template.getName();
      Request request = new Request();
      request.setMethod(Method.POST);
      request.setEndpoint(String.format("/v3/templates/%s/versions", templateId));
      request.setBody("{}");
      Response response = sendGrid.api(request);
      if (response.getStatusCode() >= 400) {
        throw new SendGridSyncException("Failed to publish template version: " + response.getBody());
      }
      log.info("Published SendGrid version for template {}", template.getName());
    } catch (IOException ex) {
      throw new SendGridSyncException("SendGrid version publish failed", ex);
    }
  }

  public Map<String, Object> defaultHeaders() {
    return Map.of("from", properties.defaultFromEmail(), "fromName", properties.defaultFromName());
  }
}
