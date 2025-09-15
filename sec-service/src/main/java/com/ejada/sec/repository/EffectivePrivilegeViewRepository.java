package com.ejada.sec.repository;

import com.ejada.sec.domain.EffectivePrivilegeProjection;
import com.ejada.sec.domain.Privilege;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.UUID;

// Spring Data requires a domain type to create a repository bean.  Although the
// queries in this repository operate on the `effective_privileges` view and
// return `EffectivePrivilegeProjection`, we associate the repository with the
// `Privilege` entity so that Spring can create a proxy implementation.
public interface EffectivePrivilegeViewRepository extends Repository<Privilege, Long> {

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
