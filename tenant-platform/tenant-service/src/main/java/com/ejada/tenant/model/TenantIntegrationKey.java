package com.ejada.tenant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder;
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
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TenantIntegrationKey {

    public static final int KEY_ID_LENGTH = 64;
    public static final int SECRET_LENGTH = 255;
    public static final int LABEL_LENGTH = 128;
    public static final int STATUS_LENGTH = 16;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tik_id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private Long tikId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_tik_tenant"))
    private Tenant tenant;

    @Column(name = "key_id", length = KEY_ID_LENGTH, nullable = false)
    private String keyId; // public identifier

    @Column(name = "key_secret", length = SECRET_LENGTH, nullable = false)
    private String keySecret; // store HASH (bcrypt/argon2), never plaintext

    @Column(name = "label", length = LABEL_LENGTH)
    private String label;

    // ---------- Scopes ----------
    // Option A: native Postgres text[] (requires hibernate-types)
    // @Type(StringArrayType.class)
    // @Column(name = "scopes", columnDefinition = "text[]")
    // private String[] scopes;

    // Option B: store as JSON string and convert in code
    @Column(name = "scopes", columnDefinition = "text[]")
    private String[] scopes;

    @Column(name = "status", length = STATUS_LENGTH, nullable = false)
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

    public final Tenant getTenant() {
        return tenant == null ? null : Tenant.ref(tenant.getId());
    }

    public final void setTenant(final Tenant tenant) {
        this.tenant = tenant == null ? null : Tenant.ref(tenant.getId());
    }

    public final String[] getScopes() {
        return scopes == null ? null : scopes.clone();
    }

    public final void setScopes(final String[] scopes) {
        this.scopes = scopes == null ? null : scopes.clone();
    }

    public final boolean isDeleted() {
        return Boolean.TRUE.equals(isDeleted);
    }

    public final boolean isActive() {
        return Status.ACTIVE.equals(status);
    }

    public final boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(java.time.OffsetDateTime.now());
    }

    /** id-only reference helper */
    public static TenantIntegrationKey ref(final Long id) {
        if (id == null) {
            return null;
        }
        TenantIntegrationKey tik = new TenantIntegrationKey();
        tik.setTikId(id);
        return tik;
    }

    public enum Status {
        ACTIVE, SUSPENDED, REVOKED, EXPIRED
    }
}
