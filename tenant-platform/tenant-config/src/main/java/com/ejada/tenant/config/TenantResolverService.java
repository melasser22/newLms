package com.ejada.tenant.config;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map; import java.util.UUID;
@Service
public class TenantResolverService {
  private final NamedParameterJdbcTemplate jdbc;
  public record TenantRow(UUID id, String slug, String status) {}
  public TenantResolverService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }
  @Cacheable(value = "tenant_slug_cache", key = "#key", unless = "#result == null")
  public TenantRow resolve(String key) {
    String sql = "select tenant_id as id, tenant_slug as slug, status::text as status " +
      "from tenant where (tenant_slug = :k or :k = any(domains)) and status='ACTIVE'::tenant_status";
    return jdbc.query(sql, Map.of("k", key), rs -> rs.next()
      ? new TenantRow((UUID) rs.getObject("id"), rs.getString("slug"), rs.getString("status")) : null);
  }
}
