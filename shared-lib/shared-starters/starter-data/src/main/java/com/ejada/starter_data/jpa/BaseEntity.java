package com.ejada.starter_data.jpa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Objects;

/**
 * Common JPA base:
 *  - Long surrogate key (IDENTITY)
 *  - createdAt / updatedAt (Instant)
 *  - optimistic locking via @Version
 *  - safe, idempotent lifecycle hooks
 */
@MappedSuperclass
public abstract class BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Set once on insert. */
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  /** Updated on each change. */
  @Column(nullable = false)
  private Instant updatedAt;

  /** Optimistic locking field. */
  @Version
  @Column(nullable = false)
  private long version;

  /* ------------ JPA lifecycle ------------ */

  @PrePersist
  protected void onCreate() {
    final Instant now = Instant.now();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    // always initialize updatedAt on first persist
    this.updatedAt = now;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }

  /* ------------ Convenience ------------ */

  /** @return true if this entity hasn't been persisted yet. */
  @Transient
  public boolean isNew() {
    return this.id == null;
  }

  /* ------------ Getters/Setters ------------ */

  public Long getId() { return id; }
  /** Protected for ORM/testing; generally do not set IDs manually. */
  protected void setId(Long id) { this.id = id; }

  public Instant getCreatedAt() { return createdAt; }
  protected void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  protected void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

  public long getVersion() { return version; }
  protected void setVersion(long version) { this.version = version; }

  /* ------------ Equality (by id) ------------ */

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BaseEntity that = (BaseEntity) o;
    // if either id is null, fall back to reference equality
    return id != null && Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    // if id is null, use identity hash to avoid cross-session collisions
    return (id == null) ? System.identityHashCode(this) : id.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{id=" + id + ", version=" + version + "}";
  }
}
