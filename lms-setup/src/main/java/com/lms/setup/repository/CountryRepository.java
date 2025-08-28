package com.lms.setup.repository;

import com.lms.setup.model.Country;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer>, JpaSpecificationExecutor<Country> {

    Optional<Country> findByCountryCdIgnoreCase(String countryCd);

    boolean existsByCountryCdIgnoreCase(String countryCd);

    List<Country> findByIsActiveTrueOrderByCountryEnNmAsc();

    Page<Country> findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(
            String enNm, String arNm, Pageable pageable
    );

    // non-paged overload for "all=true"
    List<Country> findByCountryEnNmContainingIgnoreCaseOrCountryArNmContainingIgnoreCase(
            String en, String ar, Sort sort);
}
