package com.duong.post.repository.http;

import com.duong.post.dto.ApiResponse;
import com.duong.post.dto.response.UserFollowingResponse;
import com.duong.post.dto.response.UserProfileResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "profile-service", url = "${app.services.profile.url}")
public interface ProfileClient {
    @GetMapping("/internal/users/{userId}")
    ApiResponse<UserProfileResponse> getProfile(@PathVariable String userId);

    // trả về danh sách userId mà current user đang follow
    @GetMapping("/internal/users/{userId}/followings")
    ApiResponse<List<UserFollowingResponse>> getFollowings(@PathVariable("userId") String userId);
}
