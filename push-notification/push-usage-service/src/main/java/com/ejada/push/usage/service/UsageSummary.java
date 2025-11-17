package com.ejada.push.usage.service;

public record UsageSummary(String templateKey, long sent, long delivered, long opened) {}
