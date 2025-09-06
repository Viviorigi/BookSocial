package com.duong.profile.mapper;

import com.duong.profile.dto.request.ProfileCreationRequest;
import com.duong.profile.dto.request.UpdateProfileRequest;
import com.duong.profile.dto.response.UserProfileResponse;
import com.duong.profile.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfile toUserProfile(ProfileCreationRequest profileCreationRequest);

    UserProfileResponse toUserProfileResponse(UserProfile userProfile);

    void update(@MappingTarget UserProfile entity, UpdateProfileRequest request);
}
