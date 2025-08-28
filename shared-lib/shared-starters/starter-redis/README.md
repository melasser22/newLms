# starter-redis

Properties:
```yaml
shared:
  redis:
    host: localhost
    port: 6379
    password: ""
    ssl: false
    database: 0

    cache-enabled: true
    default-ttl: 10m
    cache-key-prefix: "shared::"

    session:
      enabled: false
      timeout: 30m
      namespace: "shared:session:"
```
Beans:
- `LettuceConnectionFactory`
- `RedisTemplate<String,String>` (named: `stringRedisTemplate`)
- `RedisCacheManager` (with TTL & prefix)
- Optional Spring Session via `@EnableRedisHttpSession` when on classpath
