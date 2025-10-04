package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends TenantAwareRepository<Role, Long> {

    @Override
    @Query("SELECT r FROM Role r WHERE r.id = :id AND r.tenantId = :tenantId")
    Optional<Role> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Role r " +
           "WHERE r.id = :id AND r.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId")
    List<Role> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId")
    List<Role> findAllByTenantId(@Param("tenantId") UUID tenantId, Sort sort);

    @Override
    @Query("SELECT r FROM Role r WHERE r.tenantId = :tenantId")
    Page<Role> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Override
    @Query("SELECT COUNT(r) FROM Role r WHERE r.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM Role r WHERE r.id = :id AND r.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM Role r WHERE r.tenantId = :tenantId")
    void deleteAllByTenantId(@Param("tenantId") UUID tenantId);

    Optional<Role> findByTenantIdAndCode(UUID tenantId, String code);

    boolean existsByTenantIdAndCode(UUID tenantId, String code);
}
