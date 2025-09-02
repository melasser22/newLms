package com.ejada.openapi.starter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(SharedOpenApiProperties.class)
@ConditionalOnProperty(prefix = "shared.openapi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SharedOpenApiAutoConfiguration {

  @ConditionalOnMissingBean
  public OpenAPI openAPI(SharedOpenApiProperties props) {
    OpenAPI api = new OpenAPI()
        .info(new Info()
            .title(props.getTitle())
            .version(props.getVersion())
            .description(props.getDescription()));

    if (!props.getServers().isEmpty()) {
      api.setServers(props.getServers().stream().map(url -> new Server().url(url)).toList());
    }

    if (props.isJwtSecurity()) {
      var schemeName = "bearer-jwt";
      api.schemaRequirement(schemeName, new SecurityScheme()
          .name(schemeName)
          .type(SecurityScheme.Type.HTTP)
          .scheme("bearer")
          .bearerFormat("JWT"));
      api.addSecurityItem(new SecurityRequirement().addList(schemeName));
    }
    return api;
  }

  @ConditionalOnMissingBean(name = "sharedGroupedOpenApi")
  @ConditionalOnProperty(prefix = "shared.openapi.group", name = "enabled", havingValue = "true", matchIfMissing = true)
  public GroupedOpenApi sharedGroupedOpenApi(SharedOpenApiProperties props) {
    var builder = GroupedOpenApi.builder()
        .group(props.getGroup().getName())
        .pathsToMatch(props.getGroup().getPaths().toArray(String[]::new));
    if (!props.getGroup().getPackagesToScan().isEmpty()) {
      builder.packagesToScan(props.getGroup().getPackagesToScan().toArray(String[]::new));
    }
    return builder.build();
  }
}
