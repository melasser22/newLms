package com.ejada.sec.repository;

import com.ejada.sec.domain.UserPrivilege;
import com.ejada.sec.domain.UserPrivilegeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPrivilegeRepository extends JpaRepository<UserPrivilege, UserPrivilegeId> {

    List<UserPrivilege> findAllByIdUserId(Long userId);

    boolean existsByIdUserIdAndIdPrivilegeId(Long userId, Long privilegeId);

    /**
     * Finds a user-specific privilege override by privilege code.
     */
    @Query("SELECT up FROM UserPrivilege up " +
           "JOIN FETCH up.privilege p " +
           "WHERE up.user.id = :userId AND p.code = :privilegeCode")
    Optional<UserPrivilege> findByUserIdAndPrivilegeCode(
        @Param("userId") Long userId,
        @Param("privilegeCode") String privilegeCode
    );

    /**
     * Retrieves all privilege overrides for a user with privilege details.
     */
    @Query("SELECT up FROM UserPrivilege up " +
           "JOIN FETCH up.privilege " +
           "WHERE up.user.id = :userId")
    List<UserPrivilege> findByUserId(@Param("userId") Long userId);
}
