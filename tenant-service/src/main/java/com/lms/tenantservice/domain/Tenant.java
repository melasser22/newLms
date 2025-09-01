package com.lms.tenantservice.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tenant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {
    @Id
    private UUID id;

    private String name;

    @Column(unique = true)
    private String slug;

    @Enumerated(EnumType.STRING)
    private TenantStatus status;

    private String locale;

    private String timezone;

    @ElementCollection
    @CollectionTable(name = "tenant_domain", joinColumns = @JoinColumn(name = "tenant_id"))
    @Column(name = "domain")
    @Builder.Default
    private Set<String> domains = new HashSet<>();
}
