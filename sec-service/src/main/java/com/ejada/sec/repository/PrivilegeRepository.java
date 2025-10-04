package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.Privilege;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PrivilegeRepository extends TenantAwareRepository<Privilege, Long> {

    @Override
    @Query("SELECT p FROM Privilege p WHERE p.id = :id AND p.tenantId = :tenantId")
    Optional<Privilege> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Privilege p " +
           "WHERE p.id = :id AND p.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT p FROM Privilege p WHERE p.tenantId = :tenantId")
    java.util.List<Privilege> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT p FROM Privilege p WHERE p.tenantId = :tenantId")
    java.util.List<Privilege> findAllByTenantId(@Param("tenantId") UUID tenantId, Sort sort);

    @Override
    @Query("SELECT p FROM Privilege p WHERE p.tenantId = :tenantId")
    Page<Privilege> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Override
    @Query("SELECT COUNT(p) FROM Privilege p WHERE p.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM Privilege p WHERE p.id = :id AND p.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM Privilege p WHERE p.tenantId = :tenantId")
    void deleteAllByTenantId(@Param("tenantId") UUID tenantId);

    Optional<Privilege> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
