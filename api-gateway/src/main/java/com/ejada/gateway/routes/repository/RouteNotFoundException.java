package com.ejada.gateway.routes.repository;

import java.util.UUID;

public class RouteNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public RouteNotFoundException(UUID id) {
    super("Route definition " + id + " not found");
  }
}
