package com.ejada.sec.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 255)
    private String createdBy;
    
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @LastModifiedBy
    @Column(name = "updated_by", length = 255)
    private String updatedBy;
}