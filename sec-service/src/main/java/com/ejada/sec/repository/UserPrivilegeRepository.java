package com.ejada.sec.repository;

import com.ejada.sec.domain.UserPrivilege;
import com.ejada.sec.domain.UserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserPrivilegeRepository extends JpaRepository<UserPrivilege, UserPrivilegeId> {

    List<UserPrivilege> findAllByIdUserId(Long userId);

    boolean existsByIdUserIdAndIdPrivilegeId(Long userId, Long privilegeId);
}
