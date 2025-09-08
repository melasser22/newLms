package com.ejada.setup.repository;

import com.ejada.setup.model.City;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Integer>, JpaSpecificationExecutor<City> {

    List<City> findByCountryCountryIdAndIsActiveTrue(Integer countryId);
    Page<City> findByIsActiveTrue(Pageable pageable);
    List<City> findByCountryCountryIdAndIsActiveTrueOrderByCityEnNmAsc(Integer countryId);

    // used by list(..., all=true, q != blank)
    List<City> findByCityEnNmContainingIgnoreCaseOrCityArNmContainingIgnoreCase(
            String en, String ar, Sort sort);
}
