package com.duong.identity.mapper;

import org.mapstruct.Mapper;

import com.duong.identity.dto.request.PermissionRequest;
import com.duong.identity.dto.response.PermissionResponse;
import com.duong.identity.entity.Permission;

@Mapper(componentModel = "spring")
public interface PermissionMapper {
    Permission toPermission(PermissionRequest request);

    PermissionResponse toPermissionResponse(Permission permission);
}
