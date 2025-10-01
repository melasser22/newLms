package com.ejada.tenant.repository;


import com.ejada.tenant.model.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Integer> {

    Optional<Tenant> findByIdAndIsDeletedFalse(Integer id);

    Page<Tenant> findByIsDeletedFalse(Pageable pageable);

    Page<Tenant> findByActiveAndIsDeletedFalse(boolean active, Pageable pageable);

    Page<Tenant> findByNameContainingIgnoreCaseAndIsDeletedFalse(String name, Pageable pageable);

    Page<Tenant> findByNameContainingIgnoreCaseAndActiveAndIsDeletedFalse(String name, boolean active, Pageable pageable);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Integer id);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Integer id);

    boolean existsByCodeAndIsDeletedFalse(String code);

    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);

    boolean existsByCodeAndIsDeletedFalseAndIdNot(String code, Integer id);

    boolean existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(String name, Integer id);
}
