# starter-crypto

Auto-configures crypto services and metrics.

## Use cases
- Provides `CryptoService` and `JwtTokenService` beans.
- MDC filter to log encryption identifiers.
- Optional in-memory key provider for development.

## Usage
```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-crypto</artifactId>
</dependency>
```

Example properties:

```yaml
shared:
  crypto:
    jwt:
      secret: top-secret-key
```
