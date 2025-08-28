package com.lms.setup.repository;

import com.lms.setup.model.Lookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Integer> {

    // This matches: findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc(String)
    List<Lookup> findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc(String lookupGroupCode);
}
