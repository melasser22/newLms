# starter-security

Provides JWT-based resource server defaults and common security headers.

## Configuration Properties
```yaml
shared:
  security:
    mode: hs256|jwks|issuer
    hs256:
      secret: <required>
    resource-server:
      enabled: true
      permit-all: /actuator/health
      disable-csrf: true
      stateless: true
```

## Usage
Add the dependency:
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-security</artifactId>
</dependency>
```
