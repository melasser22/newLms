package com.ejada.template.service.support;

import com.ejada.template.domain.entity.TemplateVersionEntity;
import com.ejada.template.dto.TemplateValidationResponse;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

@Component
public class TemplateValidator {

  public TemplateValidationResponse validate(
      TemplateVersionEntity version, Map<String, Object> dynamicData) {
    Set<String> allowed = version.getAllowedVariables();
    Set<String> provided = dynamicData != null ? dynamicData.keySet() : Set.of();

    Set<String> missing = new TreeSet<>(allowed);
    missing.removeAll(provided);

    Set<String> unexpected = new TreeSet<>(provided);
    unexpected.removeAll(allowed);

    return TemplateValidationResponse.builder()
        .valid(missing.isEmpty() && unexpected.isEmpty())
        .missingVariables(missing)
        .unexpectedVariables(unexpected)
        .build();
  }
}
