package com.ejada.management.dto;

import java.util.Map;

public record GraphQlResponse(Map<String, Object> data) {

  public GraphQlResponse {
    data = data == null ? Map.of() : Map.copyOf(data);
  }
}
