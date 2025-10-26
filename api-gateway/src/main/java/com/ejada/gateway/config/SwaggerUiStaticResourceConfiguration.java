package com.ejada.gateway.config;

import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Exposes the Swagger UI static assets when running the reactive gateway.
 * <p>
 * Springdoc provides the WebFlux auto-configuration, however the gateway
 * previously did not expose the {@code /swagger-ui/**} resources because the
 * application does not serve standard static content. This configuration bridges
 * the packaged Swagger UI webjar so the well-known endpoints continue to work.
 */
@Configuration
@ConditionalOnClass(RouterFunction.class)
public class SwaggerUiStaticResourceConfiguration {

  private static final String SWAGGER_UI_BASE_PATH = "META-INF/resources/webjars/swagger-ui/";

  @Bean
  public RouterFunction<ServerResponse> swaggerUiRouterFunction() {
    return RouterFunctions.route()
        .GET("/swagger-ui", request -> ServerResponse.temporaryRedirect(URI.create("/swagger-ui/index.html")).build())
        .GET("/swagger-ui/", request -> ServerResponse.temporaryRedirect(URI.create("/swagger-ui/index.html")).build())
        .GET("/swagger-ui.html", request -> ServerResponse.temporaryRedirect(URI.create("/swagger-ui/index.html")).build())
        .add(RouterFunctions.resources("/swagger-ui/**", new ClassPathResource(SWAGGER_UI_BASE_PATH)))
        .build();
  }
}
