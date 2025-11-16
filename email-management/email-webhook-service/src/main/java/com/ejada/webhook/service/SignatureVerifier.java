package com.ejada.webhook.service;

public interface SignatureVerifier {
  boolean isValid(String payload, String signature, long timestamp);
}
