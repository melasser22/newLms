package com.ejada.sec.mapper;

import com.ejada.sec.domain.*;
import com.ejada.sec.dto.*;
import org.mapstruct.*;
import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface RoleMapper {

  @Mapping(target = "privileges", expression = "java(resolver.toPrivilegeCodes(role.getRolePrivileges()))")
  RoleDto toDto(Role role, @Context ReferenceResolver resolver);

  List<RoleDto> toDto(List<Role> roles, @Context ReferenceResolver resolver);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "userRoles", ignore = true)
  @Mapping(target = "rolePrivileges", ignore = true)
  Role toEntity(CreateRoleRequest req);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateEntity(@MappingTarget Role role, UpdateRoleRequest req);

  @AfterMapping
  default void linkPrivileges(@MappingTarget Role role, RoleDto dto,
                              @Context ReferenceResolver resolver) {
  }

  // Link privileges from codes (helper)
  default void setPrivilegesByCodes(Role role, List<String> privilegeCodes, UUID tenantId,
                                    @Context ReferenceResolver resolver) {
    var privs = resolver.privilegesByCodes(tenantId, privilegeCodes);
    role.setRolePrivileges(resolver.toRolePrivileges(role.getId(), privs));
  }
}
