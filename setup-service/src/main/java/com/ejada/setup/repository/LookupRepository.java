package com.ejada.setup.repository;

import com.ejada.setup.model.Lookup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LookupRepository extends JpaRepository<Lookup, Integer> {

    // This matches: findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc(String)
    List<Lookup> findByLookupGroupCodeAndIsActiveTrueOrderByLookupItemEnNmAsc(String lookupGroupCode);
}
