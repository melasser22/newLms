package com.ejada.gateway.loadbalancer;

import com.ejada.common.constants.HeaderNames;
import java.net.URI;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.RequestData;
import org.springframework.cloud.client.loadbalancer.RequestDataContext;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;

final class LoadBalancerRequestAdapter {

  private LoadBalancerRequestAdapter() {
  }

  static RequestData resolveRequestData(Request<?> request) {
    if (request instanceof DefaultRequest<?> defaultRequest) {
      Object context = defaultRequest.getContext();
      if (context instanceof RequestDataContext dataContext) {
        return dataContext.getClientRequest();
      }
    }
    if (request instanceof RequestDataContext dataContext) {
      return dataContext.getClientRequest();
    }
    if (request != null && request.getContext() instanceof RequestDataContext dataContext) {
      return dataContext.getClientRequest();
    }
    return null;
  }

  static String resolveRouteId(Request<?> request) {
    RequestData requestData = resolveRequestData(request);
    if (requestData == null) {
      return null;
    }
    Object routeAttr = requestData.getAttributes().get(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
    if (routeAttr instanceof Route route) {
      return route.getId();
    }
    if (routeAttr instanceof String text && StringUtils.hasText(text)) {
      return text;
    }
    return null;
  }

  static boolean isWebSocket(RequestData requestData) {
    if (requestData == null) {
      return false;
    }
    HttpHeaders headers = requestData.getHeaders();
    if (headers == null) {
      return false;
    }
    String upgrade = headers.getFirst(HttpHeaders.UPGRADE);
    return StringUtils.hasText(upgrade) && "websocket".equalsIgnoreCase(upgrade.trim());
  }

  static String resolveTenantId(Request<?> request, TenantContext tenantContext) {
    if (tenantContext != null) {
      String fromContext = tenantContext.currentTenantId()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .orElse(null);
      if (StringUtils.hasText(fromContext)) {
        return fromContext;
      }
    }
    RequestData requestData = resolveRequestData(request);
    if (requestData == null) {
      return null;
    }
    String tenant = requestData.getHeaders().getFirst(HeaderNames.X_TENANT_ID);
    if (StringUtils.hasText(tenant)) {
      return tenant.trim();
    }
    Object attributeTenant = requestData.getAttributes().get(HeaderNames.X_TENANT_ID);
    return attributeTenant instanceof String text && StringUtils.hasText(text) ? text.trim() : null;
  }

  static String resolveStickinessKey(RequestData requestData, String tenantId) {
    if (requestData == null) {
      return null;
    }
    HttpHeaders headers = requestData.getHeaders();
    if (headers == null) {
      return null;
    }
    String websocketKey = headers.getFirst("Sec-WebSocket-Key");
    String connectionId = headers.getFirst("X-Connection-Id");
    String base = StringUtils.hasText(websocketKey) ? websocketKey : connectionId;
    if (!StringUtils.hasText(base)) {
      URI uri = requestData.getUrl();
      base = (uri != null) ? uri.toString() : "";
    }
    String tenant = StringUtils.hasText(tenantId) ? tenantId : "anonymous";
    return tenant + ':' + base;
  }
}
