package com.ejada.push.template.service;

import java.util.Map;

public record TemplatePreview(String title, String body, String imageUrl, Map<String, String> data) {}
