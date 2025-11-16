package com.ejada.usage.dto;

import java.util.List;

public record UsageReportDto(String tenantId, List<UsageMetricDto> metrics) {}
