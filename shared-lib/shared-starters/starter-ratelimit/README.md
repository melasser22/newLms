# starter-ratelimit

Bucket4j-based rate limiting filter.

## Use cases
- Apply IP or tenant based rate limits.
- Configure limits and window durations via properties.

## Usage
```xml
<dependency>
  <groupId>com.lms</groupId>
  <artifactId>starter-ratelimit</artifactId>
</dependency>
```

Example properties:

```yaml
shared:
  ratelimit:
    enabled: true
    capacity: 100
    refill-period: 1m
```
