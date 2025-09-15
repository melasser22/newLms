package com.ejada.redis.starter.config;


@FunctionalInterface
public interface KeyPrefixStrategy {
  String resolvePrefix();   // e.g. "shared:public:" or "shared:<tenant>:"
}