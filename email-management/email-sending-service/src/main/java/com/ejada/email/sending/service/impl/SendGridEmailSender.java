package com.ejada.email.sending.service.impl;

import com.ejada.email.sending.client.TemplateClient;
import com.ejada.email.sending.client.dto.TemplateDescriptor;
import com.ejada.email.sending.config.EmailSendingProperties;
import com.ejada.email.sending.config.SendGridProperties;
import com.ejada.email.sending.dto.AttachmentMetadataDto;
import com.ejada.email.sending.messaging.EmailEnvelope;
import com.ejada.email.sending.service.AttachmentMergeService;
import com.ejada.email.sending.service.EmailSender;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.MailSettings;
import com.sendgrid.helpers.mail.objects.Setting;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.sendgrid.helpers.mail.objects.Attachments;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

@Service
@SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "Dependencies are injected and managed externally")
public class SendGridEmailSender implements EmailSender {

  private static final Logger log = LoggerFactory.getLogger(SendGridEmailSender.class);

  private final SendGrid client;
  private final SendGridProperties properties;
  private final TemplateClient templateClient;
  private final AttachmentMergeService attachmentMergeService;
  private final RestTemplate restTemplate;
  private final EmailSendingProperties emailSendingProperties;

  public SendGridEmailSender(
      SendGridProperties properties,
      TemplateClient templateClient,
      AttachmentMergeService attachmentMergeService,
      RestTemplate restTemplate,
      EmailSendingProperties emailSendingProperties) {
    this.properties = properties;
    this.client = new SendGrid(properties.getApiKey());
    this.templateClient = templateClient;
    this.attachmentMergeService = attachmentMergeService;
    this.restTemplate = restTemplate;
    this.emailSendingProperties = emailSendingProperties;
  }

  @Override
  public void send(EmailEnvelope envelope) {
    if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
      throw new IllegalStateException("SendGrid API key is not configured");
    }
    if (envelope.mode() == com.ejada.email.sending.dto.EmailSendRequest.SendMode.DRAFT) {
      log.info("Skipping send for draft email {}", envelope.id());
      return;
    }

    TemplateDescriptor template = templateClient.fetchTemplate(envelope.tenantId(), envelope.templateKey());
    List<AttachmentMetadataDto> attachments =
        attachmentMergeService.merge(template.defaultAttachments(), envelope.attachments());

    Mail mail = new Mail();
    mail.setFrom(new Email(properties.getFromEmail(), properties.getFromName()));
    mail.setTemplateId(envelope.templateKey());

    Personalization personalization = new Personalization();
    addRecipients(personalization, envelope);
    applyDynamicTemplateData(personalization, envelope.dynamicData());
    mail.addPersonalization(personalization);

    if (!CollectionUtils.isEmpty(attachments)) {
      attachments.forEach(attachment -> mail.addAttachments(resolveAttachment(attachment)));
    }

    if (envelope.mode() == com.ejada.email.sending.dto.EmailSendRequest.SendMode.TEST || template.sandboxEnabled()) {
    	MailSettings mailSettings = new MailSettings();
    	Setting sandboxMode = new Setting();
    	sandboxMode.setEnable(true);
    	mailSettings.setSandboxMode(sandboxMode);
    	mail.setMailSettings(mailSettings);
    }

    Request request = new Request();
    request.setMethod(Method.POST);
    request.setEndpoint("mail/send");
    try {
		request.setBody(mail.build());
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    try {
      Response response = client.api(request);
      if (response.getStatusCode() >= 400) {
        throw new IllegalStateException("SendGrid returned status " + response.getStatusCode());
      }
    } catch (IOException ex) {
      throw new IllegalStateException("SendGrid API call failed", ex);
    }
  }

  private void addRecipients(Personalization personalization, EmailEnvelope envelope) {
    if (emailSendingProperties.getTestOverrideEmail() != null
        && !emailSendingProperties.getTestOverrideEmail().isBlank()
        && envelope.mode() == com.ejada.email.sending.dto.EmailSendRequest.SendMode.TEST) {
      personalization.addTo(new Email(emailSendingProperties.getTestOverrideEmail()));
      return;
    }

    envelope.to().forEach(address -> personalization.addTo(new Email(address)));
    if (envelope.cc() != null) {
      envelope.cc().forEach(address -> personalization.addCc(new Email(address)));
    }
    if (envelope.bcc() != null) {
      envelope.bcc().forEach(address -> personalization.addBcc(new Email(address)));
    }
  }

  private void applyDynamicTemplateData(Personalization personalization, Map<String, Object> data) {
    if (data == null || data.isEmpty()) {
      return;
    }
    data.forEach(personalization::addDynamicTemplateData);
  }

  private Attachments resolveAttachment(AttachmentMetadataDto metadata) {
    Attachments attachments = new Attachments();
    attachments.setType(metadata.contentType());
    attachments.setFilename(metadata.fileName());
    try {
      byte[] content = restTemplate.getForObject(metadata.url(), byte[].class);
      if (content != null) {
        attachments.setContent(Base64.getEncoder().encodeToString(content));
      }
    } catch (Exception ex) {
      log.warn("Failed to download attachment {}", metadata.url(), ex);
    }
    return attachments;
  }
}
