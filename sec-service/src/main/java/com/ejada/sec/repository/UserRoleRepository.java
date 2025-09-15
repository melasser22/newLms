package com.ejada.sec.repository;

import com.ejada.sec.domain.UserRole;
import com.ejada.sec.domain.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findAllByIdUserId(Long userId);

    boolean existsByIdUserIdAndIdRoleId(Long userId, Long roleId);

    @Modifying
    @Query("delete from UserRole ur where ur.id.userId = :userId")
    int deleteByUserId(Long userId);

    @Modifying
    @Query("delete from UserRole ur where ur.id.roleId = :roleId")
    int deleteByRoleId(Long roleId);
}
