package com.ejada.push.management.dto;

public record UsageSummary(String templateKey, long sent, long delivered, long opened) {}
