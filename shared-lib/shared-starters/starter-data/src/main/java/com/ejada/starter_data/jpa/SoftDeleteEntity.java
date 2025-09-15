package com.ejada.starter_data.jpa;
import jakarta.persistence.Column; import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.SQLRestriction;
@MappedSuperclass
@SQLRestriction("deleted = false")
public abstract class SoftDeleteEntity extends BaseEntity {
  @Column(nullable=false) private boolean deleted=false;
  public boolean isDeleted(){return deleted;} public void setDeleted(boolean d){this.deleted=d;}
}
