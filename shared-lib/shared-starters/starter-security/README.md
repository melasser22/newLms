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
      disable-csrf: true  # set to false to enable CSRF with X-CSRF-Token header
      stateless: true
```

When `disable-csrf` is set to `false`, the starter:

* stores the token in a readable cookie (`XSRF-TOKEN`)
* echoes the token in the `X-CSRF-Token` response header

Clients must capture the header/cookie on the first request and send the token
back on subsequent modifying requests via the same header.

## Usage
Add the dependency:
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-security</artifactId>
</dependency>
```
