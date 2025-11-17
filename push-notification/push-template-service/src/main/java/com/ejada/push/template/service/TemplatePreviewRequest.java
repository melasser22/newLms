package com.ejada.push.template.service;

import java.util.Map;

public record TemplatePreviewRequest(String locale, Map<String, String> variables) {}
