package com.ejada.tenant.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

// Uncomment if using hibernate-types library:
// import com.vladmihalcea.hibernate.type.array.StringArrayType;
// import com.vladmihalcea.hibernate.type.json.JsonType;
// import org.hibernate.annotations.Type;

@Entity
@Table(
    name = "tenant_integration_key",
    indexes = {
        @Index(name = "idx_tik_tenant",       columnList = "tenant_id"),
        @Index(name = "idx_tik_status",       columnList = "status"),
        @Index(name = "idx_tik_expires_at",   columnList = "expires_at"),
        @Index(name = "idx_tik_valid_from",   columnList = "valid_from"),
        @Index(name = "idx_tik_last_used_at", columnList = "last_used_at")
    }
    // DB enforces partial uniqueness: (tenant_id, key_id) where is_deleted = false
)
@DynamicUpdate
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TenantIntegrationKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tik_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long tikId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_tik_tenant"))
    private Tenant tenant;

    @Column(name = "key_id", length = 64, nullable = false)
    private String keyId; // public identifier

    @Column(name = "key_secret", length = 255, nullable = false)
    private String keySecret; // store HASH (bcrypt/argon2), never plaintext

    @Column(name = "label", length = 128)
    private String label;

    // ---------- Scopes ----------
    // Option A: native Postgres text[] (requires hibernate-types)
    // @Type(StringArrayType.class)
    // @Column(name = "scopes", columnDefinition = "text[]")
    // private String[] scopes;

    // Option B: store as JSON string and convert in code
    @Column(name = "scopes", columnDefinition = "text[]")
    private String[] scopes;

    @Column(name = "status", length = 16, nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Column(name = "valid_from", nullable = false)
    private java.time.OffsetDateTime validFrom;

    @Column(name = "expires_at", nullable = false)
    private java.time.OffsetDateTime expiresAt;

    @Column(name = "last_used_at")
    private java.time.OffsetDateTime lastUsedAt;

    @Column(name = "use_count", nullable = false)
    private Long useCount = 0L;

    @Column(name = "daily_quota")
    private Integer dailyQuota;

    // ---------- Meta ----------
    // Option A: jsonb map (requires hibernate-types)
    // @Type(JsonType.class)
    // @Column(name = "meta", columnDefinition = "jsonb")
    // private Map<String, Object> meta;

    // Option B: store raw JSON string
    @Column(name = "meta", columnDefinition = "jsonb")
    private String meta;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    @Column(name = "created_at", updatable = false, insertable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private java.time.OffsetDateTime updatedAt;

    public boolean isDeleted() { return Boolean.TRUE.equals(isDeleted); }
    public boolean isActive()  { return Status.ACTIVE.equals(status); }
    public boolean isExpired() { return expiresAt != null && expiresAt.isBefore(java.time.OffsetDateTime.now()); }

    /** id-only reference helper */
    public static TenantIntegrationKey ref(final Long id) {
        if (id == null) return null;
        TenantIntegrationKey tik = new TenantIntegrationKey();
        tik.setTikId(id);
        return tik;
    }

    @Builder
    public TenantIntegrationKey(Long tikId, Tenant tenant, String keyId, String keySecret, String label,
                                String[] scopes, Status status, java.time.OffsetDateTime validFrom,
                                java.time.OffsetDateTime expiresAt, java.time.OffsetDateTime lastUsedAt,
                                Long useCount, Integer dailyQuota, String meta, Boolean isDeleted) {
        this.tikId = tikId;
        this.tenant = tenant;
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.label = label;
        this.scopes = scopes;
        this.status = (status != null) ? status : Status.ACTIVE;
        this.validFrom = (validFrom != null) ? validFrom : java.time.OffsetDateTime.now();
        this.expiresAt = expiresAt;
        this.lastUsedAt = lastUsedAt;
        this.useCount = (useCount != null) ? useCount : 0L;
        this.dailyQuota = dailyQuota;
        this.meta = meta;
        this.isDeleted = (isDeleted != null) ? isDeleted : Boolean.FALSE;
    }

    public enum Status {
        ACTIVE, SUSPENDED, REVOKED, EXPIRED
    }
}
