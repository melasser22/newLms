# starter-openapi

A lightweight Spring Boot starter that wires Springdoc OpenAPI with:
- OpenAPI bean (info, servers)
- Optional global Bearer JWT security
- Dynamic grouped APIs from `application.properties`

## Add UI in your app
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

## Example properties
```properties
shared.openapi.enabled=true
shared.openapi.title=Shared Platform APIs
shared.openapi.description=REST endpoints
shared.openapi.version=2025.08

shared.openapi.security.enabled=true
shared.openapi.security.name=bearer-jwt
shared.openapi.security.bearer-format=JWT

shared.openapi.servers[0].url=https://api.dev.example.com
shared.openapi.servers[0].description=Dev

shared.openapi.groups[0].name=public
shared.openapi.groups[0].paths-to-match=/public/**

shared.openapi.groups[1].name=admin
shared.openapi.groups[1].paths-to-match=/admin/**,/management/**
shared.openapi.groups[1].packages-to-scan=com.shared.admin.api
```
