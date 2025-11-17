package com.ejada.push.template.model;

import java.util.Map;

public class TemplateVersion {
  private final int version;
  private final String locale;
  private final String title;
  private final String body;
  private final String imageUrl;
  private final Map<String, String> data;
  private final boolean active;

  public TemplateVersion(
      int version, String locale, String title, String body, String imageUrl, Map<String, String> data, boolean active) {
    this.version = version;
    this.locale = locale;
    this.title = title;
    this.body = body;
    this.imageUrl = imageUrl;
    this.data = data;
    this.active = active;
  }

  public int getVersion() {
    return version;
  }

  public String getLocale() {
    return locale;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Map<String, String> getData() {
    return data;
  }

  public boolean isActive() {
    return active;
  }

  public TemplateVersion withActive(boolean newActive) {
    return new TemplateVersion(version, locale, title, body, imageUrl, data, newActive);
  }
}
