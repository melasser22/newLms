package com.ejada.tenant.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

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
@Getter @Setter @NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(name = "code", length = 64, nullable = false)
    private String code;

    @Column(name = "name", length = 128, nullable = false)
    private String name;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 32)
    private String contactPhone;

    @Column(name = "logo_url", length = 255)
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

    public boolean isActive()  { return Boolean.TRUE.equals(active); }
    public boolean isDeleted() { return Boolean.TRUE.equals(isDeleted); }

    /** id-only reference helper */
    public static Tenant ref(final Integer id) {
        if (id == null) return null;
        Tenant t = new Tenant();
        t.setId(id);
        return t;
    }

    @Builder
    public Tenant(Integer id, String code, String name,
                  String contactEmail, String contactPhone, String logoUrl,
                  Boolean active, Boolean isDeleted) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.logoUrl = logoUrl;
        this.active = (active != null) ? active : Boolean.TRUE;
        this.isDeleted = (isDeleted != null) ? isDeleted : Boolean.FALSE;
    }
}
