package com.ejada.tenant.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tenant_integration_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString
public class TenantIntegrationKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String tenantCode;

    @Column(nullable = false, length = 100)
    private String integrationType;

    @Column(nullable = false, length = 500)
    private String integrationKey;
}
