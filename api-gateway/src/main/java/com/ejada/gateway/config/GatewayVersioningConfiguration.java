package com.ejada.gateway.config;

import com.ejada.gateway.versioning.VersionMappingResolver;
import com.ejada.gateway.versioning.VersionNormalizationFilter;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

/**
 * Wires the central API versioning components so they can be reused across route definitions and
 * documentation tooling.
 */
@Configuration
@EnableConfigurationProperties(GatewayVersioningProperties.class)
public class GatewayVersioningConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(GatewayVersioningConfiguration.class);

  @Bean
  public VersionMappingResolver versionMappingResolver(GatewayVersioningProperties properties) {
    return new VersionMappingResolver(properties);
  }

  @Bean
  public VersionNormalizationFilter versionNormalizationFilter(VersionMappingResolver resolver) {
    return new VersionNormalizationFilter(resolver);
  }

  /**
   * Dynamically registers grouped OpenAPI definitions per API version (when springdoc is on the
   * classpath).
   */
  @Bean
  @ConditionalOnClass(GroupedOpenApi.class)
  @ConditionalOnBean(VersionMappingResolver.class)
  public VersionedOpenApiRegistrar versionedOpenApiRegistrar(GatewayVersioningProperties properties,
      GenericApplicationContext context,
      ObjectProvider<GroupedOpenApi> groupedOpenApis) {
    return new VersionedOpenApiRegistrar(properties, context, groupedOpenApis);
  }

  static final class VersionedOpenApiRegistrar implements SmartInitializingSingleton {

    private final GatewayVersioningProperties properties;
    private final GenericApplicationContext context;
    private final ObjectProvider<GroupedOpenApi> existingGroups;

    VersionedOpenApiRegistrar(GatewayVersioningProperties properties,
        GenericApplicationContext context,
        ObjectProvider<GroupedOpenApi> existingGroups) {
      this.properties = properties;
      this.context = context;
      this.existingGroups = existingGroups;
    }

    @Override
    public void afterSingletonsInstantiated() {
      if (properties.getMappings().isEmpty()) {
        return;
      }
      Set<String> existing = new LinkedHashSet<>();
      existingGroups.forEach(group -> existing.add(group.getGroup()));

      properties.getMappings().forEach(mapping -> mapping.getRoutes().forEach(route -> {
        String version = route.getTargetVersionOrSelf();
        if (version == null) {
          return;
        }
        String groupName = StringUtils.hasText(route.getDocumentationGroup())
            ? route.getDocumentationGroup()
            : mapping.getId() + "-" + version;
        if (existing.contains(groupName) || context.containsBean(groupName)) {
          return;
        }
        LOGGER.info("Registering OpenAPI group {} for mapping {}", groupName, mapping.getId());
        String[] docPaths = mapping.getLegacyPaths().stream()
            .map(this::toDocPattern)
            .toArray(String[]::new);
        context.registerBean(groupName, GroupedOpenApi.class, () -> GroupedOpenApi.builder()
            .group(groupName)
            .pathsToMatch(docPaths)
            .build());
        existing.add(groupName);
      }));
    }

    private String toDocPattern(String path) {
      if (!StringUtils.hasText(path)) {
        return path;
      }
      String result = path;
      result = result.replaceAll("\\{\\*[^/]+}", "**");
      result = result.replaceAll("\\{([^}/]+)}", "{$1}");
      return result;
    }
  }
}
