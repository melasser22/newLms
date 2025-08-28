package com.shared.openapi.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "shared.openapi")
public class SharedOpenApiProperties {
  private boolean enabled = true;
  private String title = "Shared Service API";
  private String version = "v1";
  private String description = "OpenAPI documentation for Shared service";
  private List<String> servers = new ArrayList<>();
  private boolean jwtSecurity = true;
  private Group group = new Group();

  public static class Group {
    private String name = "shared";
    private List<String> paths = List.of("/**");
    private List<String> packagesToScan = new ArrayList<>();
    // getters/setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getPaths() { return paths; }
    public void setPaths(List<String> paths) { this.paths = paths; }
    public List<String> getPackagesToScan() { return packagesToScan; }
    public void setPackagesToScan(List<String> packagesToScan) { this.packagesToScan = packagesToScan; }
  }

  // getters/setters
  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }
  public String getVersion() { return version; }
  public void setVersion(String version) { this.version = version; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public List<String> getServers() { return servers; }
  public void setServers(List<String> servers) { this.servers = servers; }
  public boolean isJwtSecurity() { return jwtSecurity; }
  public void setJwtSecurity(boolean jwtSecurity) { this.jwtSecurity = jwtSecurity; }
  public Group getGroup() { return group; }
  public void setGroup(Group group) { this.group = group; }
}
