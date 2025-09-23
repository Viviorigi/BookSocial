package com.duong.profile.controller;

import com.duong.profile.dto.ApiResponse;
import com.duong.profile.dto.request.SearchUserRequest;
import com.duong.profile.dto.request.UpdateProfileRequest;
import com.duong.profile.dto.response.SimpleUserDtoResponse;
import com.duong.profile.dto.response.UserProfileResponse;
import com.duong.profile.service.UserProfileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserProfileController {

    UserProfileService userProfileService;

    // ===== PROFILE =====
    @GetMapping("/users/{userId}")
    public ApiResponse<UserProfileResponse> getByUserId(@PathVariable String userId) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getByUserId(userId))
                .build();
    }

    @GetMapping("/users/my-profile")
    public ApiResponse<UserProfileResponse> getMyProfile() {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.getMyProfile())
                .build();
    }

    @PutMapping("/users/my-profile")
    public ApiResponse<UserProfileResponse> updateMyProfile(@RequestBody UpdateProfileRequest request) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.updateMyProfile(request))
                .build();
    }

    @PutMapping("/users/avatar")
    public ApiResponse<UserProfileResponse> updateAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.<UserProfileResponse>builder()
                .result(userProfileService.updateAvatar(file))
                .build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ApiResponse<List<UserProfileResponse>> getAllProfiles() {
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(userProfileService.getAllProfiles())
                .build();
    }

    @PostMapping("/users/search")
    public ApiResponse<List<UserProfileResponse>> search(@RequestBody SearchUserRequest request) {
        return ApiResponse.<List<UserProfileResponse>>builder()
                .result(userProfileService.search(request))
                .build();
    }

    // ===== FOLLOW / UNFOLLOW =====
    @PostMapping("/users/{targetUserId}/follow")
    public ApiResponse<String> follow(@PathVariable String targetUserId) {
        userProfileService.follow(targetUserId);
        return ApiResponse.<String>builder().message("Followed").result("OK").build();
    }

    @DeleteMapping("/users/{targetUserId}/follow")
    public ApiResponse<String> unfollow(@PathVariable String targetUserId) {
        userProfileService.unfollow(targetUserId);
        return ApiResponse.<String>builder().message("Unfollowed").result("OK").build();
    }

    // ===== LIST followers / following =====
    @GetMapping("/users/{userId}/following")
    public ApiResponse<List<SimpleUserDtoResponse>> following(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<List<SimpleUserDtoResponse>>builder()
                .result(userProfileService.getFollowing(userId, page, size))
                .build();
    }

    @GetMapping("/users/{userId}/followers")
    public ApiResponse<List<SimpleUserDtoResponse>> followers(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<List<SimpleUserDtoResponse>>builder()
                .result(userProfileService.getFollowers(userId, page, size))
                .build();
    }
}
