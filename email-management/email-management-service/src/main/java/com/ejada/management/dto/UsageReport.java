package com.ejada.management.dto;

import java.util.List;

public record UsageReport(String tenantId, List<UsageMetric> metrics) {}
