package com.ejada.management.service;

import com.ejada.management.config.ChildServiceProperties;
import com.ejada.management.dto.UsageReport;
import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class UsageGatewayService {

  private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_DATE;

  private final ChildServiceProperties properties;
  private final ChildServiceInvoker invoker;

  public UsageGatewayService(ChildServiceProperties properties, ChildServiceInvoker invoker) {
    this.properties = properties;
    this.invoker = invoker;
  }

  public UsageReport fetchUsage(String tenantId, LocalDate from, LocalDate to) {
    URI baseUrl = requireEndpoint(properties.getUsage().getBaseUrl(), "usage");
    String path =
        String.format(
            "/api/v1/tenants/%s/usage?from=%s&to=%s",
            tenantId, format(from), format(to));
    return invoker.get(baseUrl, path, UsageReport.class);
  }

  private String format(LocalDate date) {
    return date.format(ISO);
  }

  private URI requireEndpoint(URI uri, String name) {
    Assert.notNull(uri, () -> "Missing baseUrl for " + name + " service");
    return uri;
  }
}
