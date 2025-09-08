# starter-headers

**Purpose**
- Normalize inbound headers (correlation / request / tenant / user).
- Generate correlation/request ids if missing.
- Put keys in MDC for logging.
- Add standard security headers to every response.
- Propagate selected headers on outbound WebClient calls.
- Support Forwarded/Proxy headers when behind a reverse proxy.

Add the dependency and you get consistent IDs and security headers across all services.

## Usage
```xml
<dependency>
  <groupId>com.ejada</groupId>
  <artifactId>starter-headers</artifactId>
</dependency>
```
