package com.ejada.catalog.entity;

import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantFeatureOverrideId implements Serializable {

    private UUID tenantId;
    private String featureKey;
}
