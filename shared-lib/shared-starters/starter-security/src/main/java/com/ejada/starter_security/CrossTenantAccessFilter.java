package com.ejada.starter_security;

import com.ejada.common.constants.HeaderNames;
import com.ejada.common.context.ContextManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

/**
 * Servlet filter that prevents cross-tenant access attempts by verifying the
 * tenant identifier present in the JWT/context against values found in the
 * request (header, path segments, query parameters, and JSON body fields).
 */
class CrossTenantAccessFilter extends OncePerRequestFilter {

  private static final Pattern TENANT_PATH_PATTERN = Pattern.compile("/tenants/([A-Za-z0-9_-]{1,64})");

  private final SharedSecurityProps.TenantVerification verification;
  private final ObjectMapper objectMapper;

  CrossTenantAccessFilter(SharedSecurityProps.TenantVerification verification, ObjectMapper objectMapper) {
    this.verification = verification;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
    String contextTenant = normalize(ContextManager.Tenant.get());
    if (contextTenant == null) {
      filterChain.doFilter(request, response);
      return;
    }

    HttpServletRequest requestToUse = request;
    if (shouldInspectBody(request)) {
      requestToUse = new ContentCachingRequestWrapper(request);
    }

    Set<String> candidates = collectCandidateTenants(requestToUse);
    for (String candidate : candidates) {
      if (candidate != null && !candidate.equals(contextTenant)) {
        respondForbidden(response, contextTenant, candidate);
        return;
      }
    }

    filterChain.doFilter(requestToUse, response);
  }

  private boolean shouldInspectBody(HttpServletRequest request) {
    if (!"POST".equalsIgnoreCase(request.getMethod())
        && !"PUT".equalsIgnoreCase(request.getMethod())
        && !"PATCH".equalsIgnoreCase(request.getMethod())) {
      return false;
    }
    String contentType = request.getContentType();
    return contentType != null && contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE);
  }

  private Set<String> collectCandidateTenants(HttpServletRequest request) throws IOException {
    Set<String> candidates = new LinkedHashSet<>();
    candidates.add(normalize(request.getHeader(HeaderNames.X_TENANT_ID)));
    candidates.add(normalize(request.getParameter("tenantId")));
    candidates.add(normalize(request.getParameter("tenant_id")));
    String uri = request.getRequestURI();
    if (uri != null) {
      Matcher matcher = TENANT_PATH_PATTERN.matcher(uri);
      if (matcher.find()) {
        candidates.add(normalize(matcher.group(1)));
      }
    }
    if (request instanceof ContentCachingRequestWrapper wrapper) {
      byte[] body = StreamUtils.copyToByteArray(wrapper.getInputStream());
      if (body.length > 0) {
        String json = new String(body, StandardCharsets.UTF_8);
        try {
          JsonNode node = objectMapper.readTree(json);
          if (node != null) {
            String[] fields = verification.getBodyTenantFields();
            if (fields == null) {
              fields = new String[]{"tenantId", "tenant_id"};
            }
            for (String field : fields) {
              JsonNode fieldNode = node.at(jsonPointer(field));
              if (fieldNode.isMissingNode()) {
                fieldNode = node.path(field);
              }
              if (!fieldNode.isMissingNode() && fieldNode.isTextual()) {
                candidates.add(normalize(fieldNode.asText()));
              }
            }
          }
        } catch (IOException ignored) {
          // ignore malformed JSON; downstream will handle
        }
      }
    }
    candidates.remove(null);
    return candidates;
  }

  private void respondForbidden(HttpServletResponse response, String contextTenant, String provided) throws IOException {
    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    String body = String.format(
        "{\"code\":\"TENANT_CONTEXT_MISMATCH\",\"message\":\"Tenant mismatch (context=%s, provided=%s)\"}",
        contextTenant, provided);
    response.getWriter().write(body);
  }

  private static String normalize(String value) {
    if (value == null) {
      return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
  }

  private static String jsonPointer(String field) {
    if (field == null || field.isBlank()) {
      return "/tenantId";
    }
    return field.startsWith("/") ? field : "/" + field;
  }
}

