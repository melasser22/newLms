package com.ejada.setup.mapper;

import com.ejada.setup.dto.CityDto;
import com.ejada.setup.model.City;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.lang.NonNull;

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
  CityDto toDto(@NonNull City entity);

  @Mapping(source = "id",        target = "cityId")
  @Mapping(source = "cityCd",    target = "cityCd")
  @Mapping(source = "cityEnNm",  target = "cityEnNm")
  @Mapping(source = "cityArNm",  target = "cityArNm")
  @Mapping(source = "isActive",  target = "isActive")
  @Mapping(source = "countryId", target = "countryId")
  City toEntity(@NonNull CityDto dto);

  //  Add this so MapStruct generates the iterable mapper
  List<CityDto> toDtoList(@NonNull List<City> entities);

  // (optional, if you ever need reverse list mapping)
  // List<City> toEntityList(List<CityDto> dtos);

  // Uses the list mapper above
  default Page<CityDto> toDtoPage(Page<City> page) {
    if (page == null) {
      return Page.empty();
    }
    List<CityDto> content = toDtoList(page.getContent());
    return new PageImpl<>(content, page.getPageable(), page.getTotalElements());
  }
}
