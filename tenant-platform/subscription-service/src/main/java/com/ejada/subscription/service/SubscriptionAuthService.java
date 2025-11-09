package com.ejada.subscription.service;

public interface SubscriptionAuthService {
  String authenticate(String loginName, String sha256Password);
}
