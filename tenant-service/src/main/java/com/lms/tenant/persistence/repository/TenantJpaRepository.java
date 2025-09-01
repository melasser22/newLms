package com.lms.tenant.persistence.repository;

import com.lms.tenant.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantJpaRepository extends JpaRepository<TenantEntity, UUID> {

    /**
     * Finds a tenant by its unique slug.
     *
     * @param slug The tenant slug.
     * @return An Optional containing the tenant if found.
     */
    Optional<TenantEntity> findBySlug(String slug);
}
