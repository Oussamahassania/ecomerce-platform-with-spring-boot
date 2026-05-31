package com.ecomerce.ecomerce_web.mapper;

import com.ecomerce.ecomerce_web.dtos.UserRequestDto;
import com.ecomerce.ecomerce_web.dtos.UserResponseDto;
import com.ecomerce.ecomerce_web.entity.Role;
import com.ecomerce.ecomerce_web.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "role", source = "role")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserRequestDto dto, Role role);
    @Mapping(source = "role.name",target = "roleName")
    UserResponseDto toDto(User user);
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UserRequestDto dto, @MappingTarget User user);

}
