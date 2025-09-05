package com.ejada.setup.repository;

import com.ejada.setup.model.SystemParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SystemParameterRepository extends JpaRepository<SystemParameter, Integer>, JpaSpecificationExecutor<SystemParameter> {

    Optional<SystemParameter> findByParamKeyIgnoreCase(String paramKey);

    boolean existsByParamKeyIgnoreCase(String paramKey);

    Page<SystemParameter> findByParamGroupIgnoreCase(String paramGroup, Pageable pageable);

    List<SystemParameter> findByParamKeyIn(Collection<String> keys);

    Page<SystemParameter> findByIsActiveTrue(Pageable pageable);
    
    Optional<SystemParameter> findByParamKey(String paramKey);
    


}
