package com.ejada.sec.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import com.ejada.sec.domain.EffectivePrivilegeProjection;

import java.util.List;
import java.util.UUID;

public interface EffectivePrivilegeViewRepository extends Repository<Object, Long> {

    @Query(value = """
        select user_id     as userId,
               code        as code,
               resource    as resource,
               action      as action,
               is_effective as isEffective
        from effective_privileges
        where user_id = :userId
          and tenant_id = :tenantId
          and is_effective = true
        """, nativeQuery = true)
    List<EffectivePrivilegeProjection> findEffectiveByUserAndTenant(Long userId, UUID tenantId);
}
