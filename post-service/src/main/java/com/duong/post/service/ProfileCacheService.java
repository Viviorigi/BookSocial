package com.duong.post.service;

import com.duong.post.dto.response.UserProfileResponse;
import com.duong.post.repository.http.ProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileCacheService {
    private final ProfileClient profileClient;

    @Cacheable(cacheNames = "profile:by-id", key = "#userId", unless = "#result == null")
    public UserProfileResponse getProfile(String userId) {
        try {
            var res = profileClient.getProfile(userId);
            return (res != null) ? res.getResult() : null;
        } catch (Exception ex) {
            return null;
        }
    }


}
