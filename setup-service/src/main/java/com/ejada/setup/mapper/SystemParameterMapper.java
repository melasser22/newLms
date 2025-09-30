package com.ejada.setup.mapper;

import com.ejada.setup.dto.SystemParameterRequest;
import com.ejada.setup.dto.SystemParameterResponse;
import com.ejada.setup.model.SystemParameter;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.lang.NonNull;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SystemParameterMapper {

    SystemParameter toEntity(@NonNull SystemParameterRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@NonNull SystemParameterRequest request, @MappingTarget SystemParameter entity);

    SystemParameterResponse toResponse(@NonNull SystemParameter entity);

    List<SystemParameterResponse> toResponseList(@NonNull List<SystemParameter> entities);

    default Page<SystemParameterResponse> toResponsePage(Page<SystemParameter> page) {
        if (page == null) {
            return Page.empty();
        }
        return new PageImpl<>(toResponseList(page.getContent()), page.getPageable(), page.getTotalElements());
    }
}
