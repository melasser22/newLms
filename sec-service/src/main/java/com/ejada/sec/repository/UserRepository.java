package com.ejada.sec.repository;

import com.ejada.sec.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTenantIdAndUsername(UUID tenantId, String username);

    Optional<User> findByTenantIdAndEmail(UUID tenantId, String email);

    boolean existsByTenantIdAndUsername(UUID tenantId, String username);

    boolean existsByTenantIdAndEmail(UUID tenantId, String email);

    List<User> findAllByTenantId(UUID tenantId);
}
