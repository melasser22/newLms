package com.ejada.setup.mapper;

import com.ejada.setup.dto.CountryDto;
import com.ejada.setup.model.Country;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.lang.NonNull;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CountryMapper {

    Country toEntity(@NonNull CountryDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@NonNull CountryDto dto, @MappingTarget Country entity);

    CountryDto toDto(@NonNull Country entity);

    List<CountryDto> toDtoList(@NonNull List<Country> entities);

    default Page<CountryDto> toDtoPage(Page<Country> page) {
        if (page == null) {
            return Page.empty();
        }
        return new PageImpl<>(toDtoList(page.getContent()), page.getPageable(), page.getTotalElements());
    }
}
