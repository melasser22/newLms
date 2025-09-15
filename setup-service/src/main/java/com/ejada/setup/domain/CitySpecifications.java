package com.ejada.setup.domain;

import com.ejada.setup.model.City;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.util.Locale;

public class CitySpecifications {
  private CitySpecifications() { }

  public static Specification<City> isActive() {
    return (root, query, cb) -> cb.isTrue(root.get("isActive"));
  }

  public static Specification<City> nameContains(final String q) {
    if (!StringUtils.hasText(q)) {
      return null; // will be ignored when combined
    }
    String like = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    return (root, query, cb) -> {
      Predicate en = cb.like(cb.lower(root.get("cityEnNm")), like);
      Predicate ar = cb.like(cb.lower(root.get("cityArNm")), like);
      return cb.or(en, ar);
    };
  }

  public static Specification<City> byCountry(final Integer countryId) {
    if (countryId == null) {
      return null;
    }
    return (root, query, cb) -> cb.equal(root.get("country").get("countryId"), countryId);
  }
}
