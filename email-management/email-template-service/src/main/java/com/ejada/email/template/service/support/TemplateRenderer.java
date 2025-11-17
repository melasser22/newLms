package com.ejada.email.template.service.support;

import com.ejada.email.template.domain.entity.TemplateVersionEntity;
import com.ejada.email.template.dto.TemplatePreviewResponse;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import java.io.IOException;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TemplateRenderer {
  private final Handlebars handlebars = new Handlebars();

  public TemplatePreviewResponse render(TemplateVersionEntity version, Map<String, Object> model) {
    return TemplatePreviewResponse.builder()
        .htmlBody(renderBody(version.getHtmlBody(), model))
        .textBody(renderBody(version.getTextBody(), model))
        .build();
  }

  private String renderBody(String body, Map<String, Object> model) {
    if (body == null) {
      return null;
    }
    try {
      Template compiled = handlebars.compileInline(body);
      return compiled.apply(model);
    } catch (IOException ex) {
      throw new IllegalStateException("Failed to render template", ex);
    }
  }
}
