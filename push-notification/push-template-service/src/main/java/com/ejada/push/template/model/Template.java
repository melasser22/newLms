package com.ejada.push.template.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Template {
  private final String key;
  private final Map<String, List<TemplateVersion>> versionsByLocale = new ConcurrentHashMap<>();

  public Template(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public synchronized TemplateVersion addVersion(TemplateVersion version) {
    List<TemplateVersion> versions = versionsByLocale.computeIfAbsent(version.getLocale(), k -> new ArrayList<>());
    versions.add(version);
    return version;
  }

  public TemplateVersion getActiveVersion(String locale) {
    List<TemplateVersion> versions = versionsByLocale.get(locale);
    if (versions == null || versions.isEmpty()) {
      return null;
    }
    return versions.stream().filter(TemplateVersion::isActive).findFirst().orElse(versions.getLast());
  }

  public List<TemplateVersion> getVersions(String locale) {
    return versionsByLocale.get(locale);
  }

  public synchronized void replaceLocaleVersions(String locale, List<TemplateVersion> versions) {
    versionsByLocale.put(locale, new ArrayList<>(versions));
  }
}
