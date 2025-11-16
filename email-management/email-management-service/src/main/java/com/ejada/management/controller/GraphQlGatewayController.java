package com.ejada.management.controller;

import com.ejada.management.dto.GraphQlRequest;
import com.ejada.management.dto.GraphQlResponse;
import com.ejada.management.dto.TenantPortalView;
import com.ejada.management.service.AuditLogger;
import com.ejada.management.service.TenantContextHolder;
import com.ejada.management.service.TenantExperienceService;
import com.ejada.management.service.TenantRateLimiter;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/graphql")
public class GraphQlGatewayController {

  private static final Pattern TENANT_PORTAL_QUERY =
      Pattern.compile("tenantPortal\\s*\\(\\s*tenantId\\s*:\\s*\\\"(?<tenant>[^\\\"]+)\\\"(,\\s*from:\\s*\\\"(?<from>[^\\\"]+)\\\")?(,\\s*to:\\s*\\\"(?<to>[^\\\"]+)\\\")?\\s*\\)");

  private final TenantExperienceService tenantExperienceService;
  private final TenantRateLimiter rateLimiter;
  private final AuditLogger auditLogger;

  public GraphQlGatewayController(
      TenantExperienceService tenantExperienceService,
      TenantRateLimiter rateLimiter,
      AuditLogger auditLogger) {
    this.tenantExperienceService = tenantExperienceService;
    this.rateLimiter = rateLimiter;
    this.auditLogger = auditLogger;
  }

  @PostMapping
  public GraphQlResponse execute(@RequestBody GraphQlRequest request) {
    Matcher matcher = TENANT_PORTAL_QUERY.matcher(request.query());
    if (matcher.find()) {
      String tenantId = matcher.group("tenant");
      ensureTenantContextMatches(tenantId);
      LocalDate from = matcher.group("from") != null ? LocalDate.parse(matcher.group("from")) : LocalDate.now().minusDays(30);
      LocalDate to = matcher.group("to") != null ? LocalDate.parse(matcher.group("to")) : LocalDate.now();
      rateLimiter.assertWithinQuota(tenantId, "graphql-portal");
      TenantPortalView view = tenantExperienceService.buildTenantPortal(tenantId, from, to);
      auditLogger.logTenantAction(tenantId, "GRAPHQL_PORTAL", from + "-" + to);
      return new GraphQlResponse(Map.of("tenantPortal", view));
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported GraphQL query");
  }

  private void ensureTenantContextMatches(String tenantId) {
    String contextTenant = TenantContextHolder.getTenantId().orElse(null);
    if (contextTenant == null || !contextTenant.equals(tenantId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Tenant id mismatch for GraphQL request");
    }
  }
}
