package com.duong.identity.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.duong.identity.dto.request.RoleRequest;
import com.duong.identity.dto.response.RoleResponse;
import com.duong.identity.entity.Role;

@Mapper(componentModel = "spring")
public interface RoleMapper {
    @Mapping(target = "permissions", ignore = true)
    Role toRole(RoleRequest request);

    RoleResponse toRoleResponse(Role role);
}
