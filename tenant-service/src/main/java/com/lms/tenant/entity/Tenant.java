package com.lms.tenant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "tenant")
public class Tenant {
    @Id
    private UUID id;

    private String name;

    @Column(name = "overage_enabled", nullable = false)
    private boolean overageEnabled = false;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isOverageEnabled() { return overageEnabled; }
    public void setOverageEnabled(boolean overageEnabled) { this.overageEnabled = overageEnabled; }
}
