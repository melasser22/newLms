package com.ejada.gateway.routes.repository;

import java.util.UUID;

public class RouteNotFoundException extends RuntimeException {

  public RouteNotFoundException(UUID id) {
    super("Route definition " + id + " not found");
  }
}
