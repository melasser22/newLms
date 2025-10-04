package com.ejada.sec.repository;

import com.ejada.data.repository.TenantAwareRepository;
import com.ejada.sec.domain.User;
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
public interface UserRepository extends TenantAwareRepository<User, Long> {

    @Override
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.tenantId = :tenantId")
    Optional<User> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
           "WHERE u.id = :id AND u.tenantId = :tenantId")
    boolean existsByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    List<User> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    List<User> findAllByTenantId(@Param("tenantId") UUID tenantId, Sort sort);

    @Override
    @Query("SELECT u FROM User u WHERE u.tenantId = :tenantId")
    Page<User> findAllByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Override
    @Query("SELECT COUNT(u) FROM User u WHERE u.tenantId = :tenantId")
    long countByTenantId(@Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM User u WHERE u.id = :id AND u.tenantId = :tenantId")
    void deleteByIdAndTenantId(@Param("id") Long id, @Param("tenantId") UUID tenantId);

    @Override
    @Modifying
    @Query("DELETE FROM User u WHERE u.tenantId = :tenantId")
    void deleteAllByTenantId(@Param("tenantId") UUID tenantId);

    Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndUsername(UUID tenantId, String username);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    @Query("SELECT DISTINCT ur.user.id FROM UserRole ur WHERE ur.role.id = :roleId")
    List<Long> findUserIdsByRoleId(@Param("roleId") Long roleId);
}
