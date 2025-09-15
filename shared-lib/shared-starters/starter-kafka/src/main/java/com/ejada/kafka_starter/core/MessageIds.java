package com.ejada.kafka_starter.core;

import java.util.UUID;
public final class MessageIds {
  private MessageIds(){}
  public static String newId() { return UUID.randomUUID().toString(); }
}