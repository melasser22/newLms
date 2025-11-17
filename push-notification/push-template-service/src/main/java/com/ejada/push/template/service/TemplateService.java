package com.ejada.push.template.service;

import com.ejada.push.template.model.Template;
import com.ejada.push.template.model.TemplateVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TemplateService {

  private final Map<String, Map<String, Template>> templatesByTenant = new ConcurrentHashMap<>();

  public TemplateResponse upsertTemplate(String tenantId, TemplateRequest request) {
    templatesByTenant.putIfAbsent(tenantId, new ConcurrentHashMap<>());
    Map<String, Template> tenantTemplates = templatesByTenant.get(tenantId);
    Template template = tenantTemplates.computeIfAbsent(request.key(), Template::new);

    List<TemplateVersion> versionsForLocale = getVersionsForLocale(template, request.locale());
    int nextVersion = versionsForLocale.size() + 1;

    List<TemplateVersion> newVersions = new ArrayList<>();
    for (TemplateVersion existing : versionsForLocale) {
      newVersions.add(existing.withActive(false));
    }

    TemplateVersion created =
        new TemplateVersion(
            nextVersion,
            request.locale(),
            request.title(),
            request.body(),
            request.imageUrl(),
            request.data(),
            true);
    newVersions.add(created);
    template.replaceLocaleVersions(request.locale(), newVersions);

    return new TemplateResponse(template.getKey(), created.getLocale(), created.getVersion(), true);
  }

  public TemplateVersion getActiveTemplate(String tenantId, String key) {
    Template template = getTemplate(tenantId, key);
    if (template == null) {
      return null;
    }
    return template.getActiveVersion("en");
  }

  public TemplateVersion getActiveTemplate(String tenantId, String key, String locale) {
    Template template = getTemplate(tenantId, key);
    if (template == null) {
      return null;
    }
    return template.getActiveVersion(StringUtils.hasText(locale) ? locale : "en");
  }

  public TemplatePreview previewTemplate(String tenantId, String key, TemplatePreviewRequest request) {
    TemplateVersion active = getActiveTemplate(tenantId, key, request.locale());
    if (active == null) {
      return null;
    }
    Map<String, String> variables = request.variables();
    return new TemplatePreview(
        render(active.getTitle(), variables),
        render(active.getBody(), variables),
        active.getImageUrl(),
        active.getData());
  }

  private Template getTemplate(String tenantId, String key) {
    Map<String, Template> templates = templatesByTenant.get(tenantId);
    if (templates == null) {
      return null;
    }
    return templates.get(key);
  }

  private List<TemplateVersion> getVersionsForLocale(Template template, String locale) {
    List<TemplateVersion> versions = template.getVersions(locale);
    if (versions == null) {
      return new ArrayList<>();
    }
    return new ArrayList<>(versions);
  }

  private String render(String source, Map<String, String> variables) {
    if (!StringUtils.hasText(source) || variables == null || variables.isEmpty()) {
      return source;
    }
    String rendered = source;
    for (Map.Entry<String, String> entry : variables.entrySet()) {
      rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return rendered;
  }
}
