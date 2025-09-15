package com.ejada.setup.repository;

import com.ejada.setup.model.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Integer>, JpaSpecificationExecutor<Resource> {

    Optional<Resource> findByResourceCdIgnoreCase(String resourceCd);

    boolean existsByResourceCdIgnoreCase(String resourceCd);

    // Resolve an endpoint definition
    Optional<Resource> findByPathIgnoreCaseAndHttpMethodIgnoreCase(String path, String httpMethod);

    // Tree/hierarchy helpers
    List<Resource> findByParentResourceIdOrderByResourceCdAsc(Integer parentResourceId);

    Page<Resource> findByIsActiveTrue(Pageable pageable);

    Page<Resource> findByResourceEnNmContainingIgnoreCaseOrResourceArNmContainingIgnoreCase(
            String enLike, String arLike, Pageable pageable
    );
}
