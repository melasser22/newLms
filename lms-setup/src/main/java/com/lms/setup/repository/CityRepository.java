package com.lms.setup.repository;

import com.lms.setup.model.City;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository     extends JpaRepository<City, Integer>, JpaSpecificationExecutor<City> {

    List<City> findByCountry_CountryIdAndIsActiveTrue(Integer countryId);
    Page<City> findByIsActiveTrue(Pageable pageable);
    List<City> findByCountry_CountryIdAndIsActiveTrueOrderByCityEnNmAsc(Integer countryId);

    // used by list(..., all=true, q != blank)
    List<City> findByCityEnNmContainingIgnoreCaseOrCityArNmContainingIgnoreCase(
            String en, String ar, Sort sort);

    @Query("SELECT c FROM City c JOIN FETCH c.country WHERE c.cityId = :id")
    Optional<City> findByIdWithCountry(@Param("id") Integer id);
}
