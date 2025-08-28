package com.shared.audit.starter.http;

import com.shared.audit.starter.api.AuditAction;
import com.shared.audit.starter.api.AuditEvent;
import com.shared.audit.starter.api.AuditService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Minimal servlet filter that emits an ACCESS audit event for every HTTP request.
 * Supports optional header/body capture and include/exclude path rules.
 */
public class AuditWebMvcFilter implements Filter {

  private final AuditService audit;

  // Tunables (wired via setters from auto-config if present)
  private boolean includeHeaders = false;
  private boolean trackBodies = false;
  private List<String> includePaths = new ArrayList<>();
  private List<String> excludePaths = new ArrayList<>();

  // Avoid flooding logs with giant payloads
  private static final int MAX_BODY_CHARS = 4000;

  public AuditWebMvcFilter(AuditService audit) {
    this.audit = audit;
  }

  // --- public setters (auto-config may call these reflectively) ---
  public void setIncludeHeaders(boolean includeHeaders) { this.includeHeaders = includeHeaders; }
  public void setTrackBodies(boolean trackBodies) { this.trackBodies = trackBodies; }
  public void setIncludePaths(List<String> includePaths) {
    this.includePaths = includePaths != null ? new ArrayList<>(includePaths) : new ArrayList<>();
  }
  public void setExcludePaths(List<String> excludePaths) {
    this.excludePaths = excludePaths != null ? new ArrayList<>(excludePaths) : new ArrayList<>();
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    // Path filtering
    if (!shouldAudit(req)) {
      chain.doFilter(request, response);
      return;
    }

    long start = System.currentTimeMillis();

    if (trackBodies) {
      // Wrap to cache content for later inspection
      ContentCachingRequestWrapper cachingReq = new ContentCachingRequestWrapper(req);
      ContentCachingResponseWrapper cachingRes = new ContentCachingResponseWrapper(res);
      try {
        chain.doFilter(cachingReq, cachingRes);
      } finally {
        emitEventWithOptionalBodies(cachingReq, cachingRes, start);
        // important: copy cached response body back to the real response
        cachingRes.copyBodyToResponse();
      }
    } else {
      try {
        chain.doFilter(request, response);
      } finally {
        emitEventNoBodies(req, res, start);
      }
    }
  }

  // ----- emitters -----

  private void emitEventNoBodies(HttpServletRequest req, HttpServletResponse res, long start) {
    Map<String, Object> meta = baseMeta(req, res, start);

    if (includeHeaders) {
      meta.put("reqHeaders", headersMap(req));
      meta.put("resHeaders", responseHeadersMap(res));
    }

    AuditEvent.Builder builder = AuditEvent.builder()
        .action(AuditAction.ACCESS)
        .resource("path", req.getRequestURI())
        .resource("method", req.getMethod())
        .resource("query", Objects.toString(req.getQueryString(), ""));

    audit.emit(applyMeta(builder, meta).build());
  }

  private void emitEventWithOptionalBodies(ContentCachingRequestWrapper req,
                                           ContentCachingResponseWrapper res,
                                           long start) {
    Map<String, Object> meta = baseMeta(req, res, start);

    if (includeHeaders) {
      meta.put("reqHeaders", headersMap(req));
      meta.put("resHeaders", responseHeadersMap(res));
    }

    // Bodies (best-effort)
    String reqBody = bytesToString(req.getContentAsByteArray(),
        charsetOrDefault(req.getCharacterEncoding()));
    String resBody = bytesToString(res.getContentAsByteArray(),
        charsetOrDefault(res.getCharacterEncoding()));

    if (!reqBody.isEmpty()) {
      meta.put("reqBody", truncate(reqBody, MAX_BODY_CHARS));
    }
    if (!resBody.isEmpty()) {
      meta.put("resBody", truncate(resBody, MAX_BODY_CHARS));
    }

    AuditEvent.Builder builder = AuditEvent.builder()
        .action(AuditAction.ACCESS)
        .resource("path", req.getRequestURI())
        .resource("method", req.getMethod())
        .resource("query", Objects.toString(req.getQueryString(), ""));

    audit.emit(applyMeta(builder, meta).build());
  }

  // ----- helpers -----

  /** Spread a meta map onto the builder (fixes "meta(Map)" compile error). */
  private static AuditEvent.Builder applyMeta(AuditEvent.Builder b, Map<String, Object> meta) {
    if (meta != null) {
      for (Map.Entry<String, Object> e : meta.entrySet()) {
        b.meta(e.getKey(), e.getValue());
      }
    }
    return b;
  }

  private Map<String, Object> baseMeta(HttpServletRequest req, HttpServletResponse res, long start) {
    Map<String, Object> meta = new LinkedHashMap<>();
    meta.put("status", res.getStatus());
    meta.put("latencyMs", System.currentTimeMillis() - start);
    meta.put("clientIp", clientIp(req));
    meta.put("scheme", req.getScheme());
    meta.put("host", req.getServerName());
    meta.put("port", req.getServerPort());
    meta.put("userAgent", Objects.toString(req.getHeader("User-Agent"), ""));
    return meta;
  }

  private boolean shouldAudit(HttpServletRequest req) {
    String path = req.getRequestURI();

    // Exclude has priority
    for (String ex : excludePaths) {
      if (matches(path, ex)) return false;
    }
    // If include list is empty -> allow all; else require a match
    if (includePaths == null || includePaths.isEmpty()) return true;
    for (String inc : includePaths) {
      if (matches(path, inc)) return true;
    }
    return false;
  }

  // Simple matcher: exact, prefix (ends with '*'), or contains '**' meaning substring
  private boolean matches(String path, String rule) {
    if (rule == null || rule.isEmpty()) return false;
    if (rule.equals(path)) return true;
    if (rule.endsWith("*")) {
      String p = rule.substring(0, rule.length() - 1);
      return path.startsWith(p);
    }
    if (rule.contains("**")) {
      String p = rule.replace("**", "");
      return path.contains(p);
    }
    return false;
  }

  private Map<String, String> headersMap(HttpServletRequest req) {
    Map<String, String> out = new LinkedHashMap<>();
    Enumeration<String> names = req.getHeaderNames();
    if (names != null) {
      while (names.hasMoreElements()) {
        String n = names.nextElement();
        out.put(n, req.getHeader(n));
      }
    }
    return out;
  }

  private Map<String, String> responseHeadersMap(HttpServletResponse res) {
    Map<String, String> out = new LinkedHashMap<>();
    for (String name : res.getHeaderNames()) {
      out.put(name, String.join(",", res.getHeaders(name)));
    }
    return out;
  }

  private String clientIp(HttpServletRequest req) {
    String h = req.getHeader("X-Forwarded-For");
    if (h != null && !h.isBlank()) {
      int comma = h.indexOf(',');
      return comma >= 0 ? h.substring(0, comma).trim() : h.trim();
    }
    h = req.getHeader("X-Real-IP");
    return (h != null && !h.isBlank()) ? h.trim() : req.getRemoteAddr();
  }

  private Charset charsetOrDefault(String enc) {
    try {
      return (enc != null && !enc.isBlank()) ? Charset.forName(enc) : StandardCharsets.UTF_8;
    } catch (Exception ignored) {
      return StandardCharsets.UTF_8;
    }
  }

  private String bytesToString(byte[] bytes, Charset cs) {
    if (bytes == null || bytes.length == 0) return "";
    return new String(bytes, cs);
  }

  private String truncate(String s, int max) {
    if (s == null) return "";
    if (s.length() <= max) return s;
    return s.substring(0, max) + "...(" + (s.length() - max) + " more chars)";
    }
}
