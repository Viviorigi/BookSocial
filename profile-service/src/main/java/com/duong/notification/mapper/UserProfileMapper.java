package com.duong.notification.mapper;

import com.duong.notification.dto.request.ProfileCreationRequest;
import com.duong.notification.dto.response.UserProfileResponse;
import com.duong.notification.entity.UserProfile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
    UserProfile toUserProfile(ProfileCreationRequest profileCreationRequest);
    UserProfileResponse toUserProfileResponse(UserProfile userProfile);

}
