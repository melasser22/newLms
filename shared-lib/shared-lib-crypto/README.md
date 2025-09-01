# shared-lib-crypto

Cryptography utilities and JWT token support.

## Use cases
- AES-GCM encryption/decryption with `CryptoFacade`.
- HMAC SHA-256 signing helpers.
- JWT token generation and validation with Spring Boot auto-configuration.

## Usage
```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>shared-lib-crypto</artifactId>
</dependency>
```

Example properties:

```yaml
shared:
  crypto:
    jwt:
      secret: top-secret-key
```

Example usage:

```java
String token = jwtTokenService.generateToken(claims);
byte[] cipher = cryptoService.encrypt("data");
```
