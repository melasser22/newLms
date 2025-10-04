package com.ejada.sec.repository;

import com.ejada.sec.domain.RolePrivilege;
import com.ejada.sec.domain.RolePrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface RolePrivilegeRepository extends JpaRepository<RolePrivilege, RolePrivilegeId> {

    List<RolePrivilege> findAllByIdRoleId(Long roleId);

    List<RolePrivilege> findAllByIdPrivilegeId(Long privilegeId);

    boolean existsByIdRoleIdAndIdPrivilegeId(Long roleId, Long privilegeId);

    /**
     * Efficiently checks if any of the given roles has the specified privilege.
     */
    @Query("SELECT CASE WHEN COUNT(rp) > 0 THEN true ELSE false END " +
           "FROM RolePrivilege rp JOIN rp.privilege p " +
           "WHERE rp.role.id IN :roleIds AND p.code = :privilegeCode")
    boolean existsByRoleIdInAndPrivilegeCode(
        @Param("roleIds") Set<Long> roleIds,
        @Param("privilegeCode") String privilegeCode
    );

    /**
     * Checks if roles have specific resource-action permission.
     */
    @Query("SELECT CASE WHEN COUNT(rp) > 0 THEN true ELSE false END " +
           "FROM RolePrivilege rp JOIN rp.privilege p " +
           "WHERE rp.role.id IN :roleIds " +
           "AND p.resource = :resource " +
           "AND p.action = :action")
    boolean existsByRoleIdInAndResourceAndAction(
        @Param("roleIds") Set<Long> roleIds,
        @Param("resource") String resource,
        @Param("action") String action
    );

    /**
     * Fetches all privileges for the given roles including privilege details.
     */
    @Query("SELECT rp FROM RolePrivilege rp " +
           "JOIN FETCH rp.privilege " +
           "WHERE rp.role.id IN :roleIds")
    List<RolePrivilege> findByRoleIdIn(@Param("roleIds") Set<Long> roleIds);

    @Modifying
    @Query("delete from RolePrivilege rp where rp.id.roleId = :roleId")
    int deleteByRoleId(Long roleId);

    @Modifying
    @Query("delete from RolePrivilege rp where rp.id.privilegeId = :privilegeId")
    int deleteByPrivilegeId(Long privilegeId);
}
