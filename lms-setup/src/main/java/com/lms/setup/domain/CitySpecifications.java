package com.lms.setup.domain;

import com.lms.setup.model.City;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class CitySpecifications {
  private CitySpecifications() {}

  public static Specification<City> isActive() {
    return (root, query, cb) -> cb.isTrue(root.get("isActive"));
  }

  public static Specification<City> nameContains(String q) {
    if (!StringUtils.hasText(q)) return null; // will be ignored when combined
    String like = "%" + q.trim().toLowerCase() + "%";
    return (root, query, cb) -> {
      Predicate en = cb.like(cb.lower(root.get("cityEnNm")), like);
      Predicate ar = cb.like(cb.lower(root.get("cityArNm")), like);
      return cb.or(en, ar);
    };
  }

  public static Specification<City> byCountry(Integer countryId) {
    if (countryId == null) return null;
    return (root, query, cb) -> cb.equal(root.get("country").get("countryId"), countryId);
  }
}
