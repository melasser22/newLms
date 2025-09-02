package com.ejada.audit.starter.core.mask;

import java.util.Map;

public interface MaskingStrategy {
  Map<String, Object> mask(String entityType, Map<String,Object> before, Map<String,Object> after);
}
