package com.ejada.sec.mapper;

import com.ejada.sec.domain.*;
import com.ejada.sec.dto.*;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(target = "roles", expression = "java(resolver.toRoleCodes(user.getRoles()))")
  @Mapping(target = "privileges", ignore = true) 
  UserDto toDto(User user, @Context ReferenceResolver resolver);

  List<UserDto> toDto(List<User> users, @Context ReferenceResolver resolver);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "passwordHash", ignore = true)
  @Mapping(target = "enabled", constant = "true")
  @Mapping(target = "locked", constant = "false")
  @Mapping(target = "lastLoginAt", ignore = true)
  @Mapping(target = "firstLoginCompleted", constant = "false")
  @Mapping(target = "passwordChangedAt", ignore = true)
  @Mapping(target = "passwordExpiresAt", ignore = true)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "refreshTokens", ignore = true)
  @Mapping(target = "resetTokens", ignore = true)
  @Mapping(target = "federatedIdentities", ignore = true)
  @Mapping(target = "userPrivileges", ignore = true)
  User toEntity(CreateUserRequest req);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  @Mapping(target = "roles", ignore = true)
  @Mapping(target = "firstLoginCompleted", ignore = true)
  @Mapping(target = "passwordChangedAt", ignore = true)
  @Mapping(target = "passwordExpiresAt", ignore = true)
  void updateEntity(@MappingTarget User user, UpdateUserRequest req);

  default void setRolesByCodes(User user, List<String> roleCodes, UUID tenantId,
                               @Context ReferenceResolver resolver) {
    var roles = resolver.rolesByCodes(tenantId, roleCodes);
    user.setRoles(resolver.toUserRoles(user.getId(), roles));
  }
}
