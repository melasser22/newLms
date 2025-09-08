package com.ejada.setup.mapper;

import com.ejada.setup.dto.ResourceDto;
import com.ejada.setup.model.Resource;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.lang.NonNull;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ResourceMapper {
    @Mapping(source = "resourceId", target = "id")
    ResourceDto toDto(@NonNull Resource entity);

    @InheritInverseConfiguration(name = "toDto")
    Resource toEntity(@NonNull ResourceDto dto);

    List<ResourceDto> toDtoList(@NonNull List<Resource> entities);

    default Page<ResourceDto> toDtoPage(Page<Resource> page) {
        if (page == null) {
            return Page.empty();
        }
        return new PageImpl<>(toDtoList(page.getContent()), page.getPageable(), page.getTotalElements());
    }
}
