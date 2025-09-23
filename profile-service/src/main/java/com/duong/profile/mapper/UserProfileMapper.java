package com.duong.profile.mapper;

import com.duong.profile.dto.request.ProfileCreationRequest;
import com.duong.profile.dto.request.UpdateProfileRequest;
import com.duong.profile.dto.response.SimpleUserDtoResponse;
import com.duong.profile.dto.response.UserProfileResponse;
import com.duong.profile.entity.UserProfile;

import org.mapstruct.Mapper;

import org.mapstruct.MappingTarget;

import java.util.Collection;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfile toUserProfile(ProfileCreationRequest req);
    void update(@MappingTarget UserProfile entity, UpdateProfileRequest req);

    // chỉ map các field cơ bản từ node
    UserProfileResponse toUserProfileResponse(UserProfile src);

    // helpers
    default List<SimpleUserDtoResponse> toSimpleList(Collection<UserProfile> list) {
        if (list == null) return List.of();
        return list.stream().map(this::toSimple).toList();
    }
    default SimpleUserDtoResponse toSimple(UserProfile u) {
        if (u == null) return null;
        return SimpleUserDtoResponse.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .avatar(u.getAvatar())
                .build();
    }
}


