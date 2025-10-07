package com.ejada.gateway.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ejada.gateway.transformation.ResponseCacheService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

class CacheRefreshServiceTest {

  private ResponseCacheService cacheService;
  private ServerProperties serverProperties;
  private ObjectProvider<WebClient.Builder> builderProvider;
  private CacheRefreshService cacheRefreshService;

  @BeforeEach
  void setUp() {
    cacheService = mock(ResponseCacheService.class, Answers.RETURNS_DEEP_STUBS);
    serverProperties = new ServerProperties();
    serverProperties.setPort(8085);
    serverProperties.getServlet().setContextPath("/gateway/api");
    builderProvider = () -> WebClient.builder();
    cacheRefreshService = new CacheRefreshService(cacheService, serverProperties, builderProvider);
  }

  @Test
  void resolveRequestUriIncludesContextPath() {
    URI uri = invokeResolveRequestUri("/catalog/plans", "tenant=alpha");
    assertThat(uri.toString()).isEqualTo("http://127.0.0.1:8085/gateway/api/catalog/plans?tenant=alpha");
  }

  @Test
  void updatesBaseUriWhenServerPortChanges() {
    TestWebServer webServer = new TestWebServer(9099);
    WebServerInitializedEvent event = new WebServerInitializedEvent(webServer, new GenericApplicationContext());

    cacheRefreshService.onWebServerInitialized(event);

    URI uri = invokeResolveRequestUri("/catalog/features", "-");
    assertThat(uri.toString()).isEqualTo("http://127.0.0.1:9099/gateway/api/catalog/features");
  }

  @Test
  void resolvesHttpsSchemeWhenSslEnabled() {
    serverProperties.getSsl().setEnabled(true);
    CacheRefreshService httpsService = new CacheRefreshService(cacheService, serverProperties, builderProvider);
    URI uri = ReflectionTestUtils.invokeMethod(httpsService, "resolveRequestUri", "/status", "-");
    assertThat(uri.toString()).isEqualTo("https://127.0.0.1:8085/gateway/api/status");
  }

  private URI invokeResolveRequestUri(String path, String query) {
    return ReflectionTestUtils.invokeMethod(cacheRefreshService, "resolveRequestUri", path, query);
  }

  private static final class TestWebServer implements org.springframework.boot.web.server.WebServer {

    private final int port;

    private TestWebServer(int port) {
      this.port = port;
    }

    @Override
    public void start() {
      // no-op for test
    }

    @Override
    public void stop() {
      // no-op for test
    }

    @Override
    public int getPort() {
      return port;
    }

    @Override
    public void shutDownGracefully(org.springframework.boot.web.server.GracefulShutdownCallback callback) {
      callback.shutdownComplete(org.springframework.boot.web.server.GracefulShutdownResult.IMMEDIATE);
    }
  }
}
