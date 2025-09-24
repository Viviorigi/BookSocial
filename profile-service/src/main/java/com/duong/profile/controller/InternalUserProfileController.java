package com.duong.profile.controller;

import com.duong.profile.dto.ApiResponse;
import com.duong.profile.dto.request.ProfileCreationRequest;
import com.duong.profile.dto.response.SimpleUserDtoResponse;
import com.duong.profile.dto.response.UserProfileResponse;
import com.duong.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class InternalUserProfileController {

    UserProfileService userProfileService;

    @PostMapping("/internal/users")
    ApiResponse<UserProfileResponse> createProfile(@RequestBody ProfileCreationRequest request){
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.createProfile(request))
                .build();
    }

    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getByUserId(userId))
                .build();
    }

    @GetMapping("/internal/users/{userId}/followings")
    public ApiResponse<List<SimpleUserDtoResponse>> following(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<List<SimpleUserDtoResponse>>builder()
                .result(userProfileService.getFollowing(userId, page, size))
                .build();
    }
}
