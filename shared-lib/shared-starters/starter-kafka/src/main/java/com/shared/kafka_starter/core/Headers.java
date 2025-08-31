package com.shared.kafka_starter.core;

public final class Headers {
  public static final String TENANT_ID = "x-tenant-id";
  public static final String CORRELATION_ID = "x-corr-id";
  public static final String MESSAGE_ID = "x-msg-id";
  public static final String SCHEMA_VERSION = "x-schema-ver";
  private Headers() {}
}