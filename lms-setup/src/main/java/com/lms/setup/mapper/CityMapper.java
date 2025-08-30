package com.lms.setup.mapper;

import com.lms.setup.dto.CityDto;
import com.lms.setup.model.City;
import com.lms.setup.model.Country;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

@SuppressFBWarnings(value = "CT_CONSTRUCTOR_THROW", justification = "Generated implementation is safe")
@Mapper(componentModel = "spring")
public interface CityMapper {

  @Mapping(source = "cityId",            target = "id")
  @Mapping(source = "cityCd",            target = "cityCd")
  @Mapping(source = "cityEnNm",          target = "cityEnNm")
  @Mapping(source = "cityArNm",          target = "cityArNm")
  @Mapping(source = "isActive",          target = "isActive")
  @Mapping(source = "country.countryId", target = "countryId")
  CityDto toDto(City entity);

  @InheritInverseConfiguration(name = "toDto")
  @Mapping(target = "country", expression = "java(toCountry(dto.getCountryId()))")
  City toEntity(CityDto dto);

  //  Add this so MapStruct generates the iterable mapper
  List<CityDto> toDtoList(List<City> entities);

  // (optional, if you ever need reverse list mapping)
  // List<City> toEntityList(List<CityDto> dtos);

  default Country toCountry(Integer id) {
    if (id == null) return null;
    Country c = new Country();
    c.setCountryId(id);
    return c;
  }

  // Uses the list mapper above
  default Page<CityDto> toDtoPage(Page<City> page) {
    if (page == null) return Page.empty();
    List<CityDto> content = toDtoList(page.getContent());
    return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
  }
}
