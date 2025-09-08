package com.ejada.sec.repository;

import com.ejada.sec.domain.RolePrivilege;
import com.ejada.sec.domain.RolePrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RolePrivilegeRepository extends JpaRepository<RolePrivilege, RolePrivilegeId> {

    List<RolePrivilege> findAllByIdRoleId(Long roleId);

    List<RolePrivilege> findAllByIdPrivilegeId(Long privilegeId);

    boolean existsByIdRoleIdAndIdPrivilegeId(Long roleId, Long privilegeId);

    @Modifying
    @Query("delete from RolePrivilege rp where rp.id.roleId = :roleId")
    int deleteByRoleId(Long roleId);

    @Modifying
    @Query("delete from RolePrivilege rp where rp.id.privilegeId = :privilegeId")
    int deleteByPrivilegeId(Long privilegeId);
}
