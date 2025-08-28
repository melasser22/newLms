package com.shared.audit.starter.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.common.exception.JsonSerializationException;

public final class JsonUtils {
  private static final ObjectMapper M = new ObjectMapper().registerModule(new JavaTimeModule());
  private JsonUtils() { }
  public static String toJson(Object o) throws JsonSerializationException {
    try {
      return M.writeValueAsString(o);
    } catch (JsonProcessingException e) {
      // propagate a checked exception for callers to handle explicitly
      throw new JsonSerializationException("Failed to serialize object to JSON", e);
    }
  }
  public static ObjectMapper mapper() { return M; }
}
