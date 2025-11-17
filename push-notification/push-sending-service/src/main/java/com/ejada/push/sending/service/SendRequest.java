package com.ejada.push.sending.service;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

public record SendRequest(
    String templateKey,
    String locale,
    Map<String, String> variables,
    String title,
    String body,
    @NotEmpty List<String> tokens) {}
