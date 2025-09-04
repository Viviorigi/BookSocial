package com.duong.identity.mapper;

import com.duong.identity.dto.request.ProfileCreationRequest;
import com.duong.identity.dto.request.UserCreationRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    ProfileCreationRequest toProfileCreationRequest(UserCreationRequest request);
}
