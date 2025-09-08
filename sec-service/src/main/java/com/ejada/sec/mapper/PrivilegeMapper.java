package com.ejada.sec.mapper;

import com.ejada.sec.domain.Privilege;
import com.ejada.sec.dto.CreatePrivilegeRequest;
import com.ejada.sec.dto.PrivilegeDto;
import com.ejada.sec.dto.UpdatePrivilegeRequest;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PrivilegeMapper {

  PrivilegeDto toDto(Privilege p);

  List<PrivilegeDto> toDto(List<Privilege> list);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "rolePrivileges", ignore = true)
  @Mapping(target = "userPrivileges", ignore = true)
  Privilege toEntity(CreatePrivilegeRequest req);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntity(@MappingTarget Privilege p, UpdatePrivilegeRequest req);
}
