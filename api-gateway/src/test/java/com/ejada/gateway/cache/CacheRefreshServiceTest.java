package com.ejada.gateway.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ejada.gateway.transformation.ResponseCacheService;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.mockito.Answers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.boot.web.context.WebServerInitializedEvent;
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
    builderProvider = new BuilderObjectProvider();
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
    WebServerApplicationContext applicationContext = mock(WebServerApplicationContext.class);
    WebServerInitializedEvent event = new TestWebServerInitializedEvent(webServer, applicationContext);

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

  private static final class BuilderObjectProvider implements ObjectProvider<WebClient.Builder> {

    @Override
    public WebClient.Builder getObject(Object... args) {
      return WebClient.builder();
    }

    @Override
    public WebClient.Builder getObject() {
      return WebClient.builder();
    }

    @Override
    public WebClient.Builder getIfAvailable() {
      return WebClient.builder();
    }

    @Override
    public WebClient.Builder getIfAvailable(Supplier<WebClient.Builder> defaultSupplier) {
      return WebClient.builder();
    }

    @Override
    public void ifAvailable(Consumer<WebClient.Builder> dependencyConsumer) {
      dependencyConsumer.accept(WebClient.builder());
    }

    @Override
    public WebClient.Builder getIfUnique() {
      return WebClient.builder();
    }

    @Override
    public WebClient.Builder getIfUnique(Supplier<WebClient.Builder> defaultSupplier) {
      return WebClient.builder();
    }

    @Override
    public void ifUnique(Consumer<WebClient.Builder> dependencyConsumer) {
      dependencyConsumer.accept(WebClient.builder());
    }

    @Override
    public Iterator<WebClient.Builder> iterator() {
      return List.of(WebClient.builder()).iterator();
    }

    @Override
    public Stream<WebClient.Builder> stream() {
      return Stream.of(WebClient.builder());
    }

    @Override
    public Stream<WebClient.Builder> orderedStream() {
      return stream();
    }
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

  private static final class TestWebServerInitializedEvent extends WebServerInitializedEvent {

    private static final long serialVersionUID = 1L;

    private final WebServerApplicationContext applicationContext;

    private TestWebServerInitializedEvent(org.springframework.boot.web.server.WebServer webServer,
        WebServerApplicationContext applicationContext) {
      super(webServer);
      this.applicationContext = applicationContext;
    }

    @Override
    public WebServerApplicationContext getApplicationContext() {
      return applicationContext;
    }
  }
}
