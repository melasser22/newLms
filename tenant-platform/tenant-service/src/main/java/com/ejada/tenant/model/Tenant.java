package com.ejada.tenant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "tenants",
    indexes = {
        @Index(name = "idx_tenants_active", columnList = "active"),
        @Index(name = "idx_tenants_created_at", columnList = "created_at")
    }
    // Note: DB has a partial unique index for (code) when is_deleted = false.
    // Avoid declaring a plain unique constraint here to not conflict with soft-delete logic.
)
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tenant {

    public static final int CODE_LENGTH = 64;
    public static final int NAME_LENGTH = 128;
    public static final int EMAIL_LENGTH = 255;
    public static final int PHONE_LENGTH = 32;
    public static final int LOGO_URL_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "code", length = CODE_LENGTH, nullable = false)
    private String code;

    @Column(name = "name", length = NAME_LENGTH, nullable = false)
    private String name;

    @Column(name = "contact_email", length = EMAIL_LENGTH)
    private String contactEmail;

    @Column(name = "contact_phone", length = PHONE_LENGTH)
    private String contactPhone;

    @Column(name = "logo_url", length = LOGO_URL_LENGTH)
    private String logoUrl;

    @Column(name = "active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = Boolean.FALSE;

    // If you mapped created_at/updated_at in DB, keep them here if you want to read them.
    @Column(name = "created_at", updatable = false, insertable = false)
    private java.time.OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private java.time.OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "tenant", fetch = FetchType.LAZY)
    private Set<TenantIntegrationKey> integrationKeys = new HashSet<>();

    public final boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public final boolean isDeleted() {
        return Boolean.TRUE.equals(isDeleted);
    }

    public final Set<TenantIntegrationKey> getIntegrationKeys() {
        return integrationKeys;
    }

    public final void setIntegrationKeys(final Set<TenantIntegrationKey> integrationKeys) {
        this.integrationKeys = integrationKeys == null ? new HashSet<>() : new HashSet<>(integrationKeys);
    }

    /** id-only reference helper */
    public static Tenant ref(final Integer id) {
        if (id == null) {
            return null;
        }
        Tenant t = new Tenant();
        t.setId(id);
        return t;
    }

}
