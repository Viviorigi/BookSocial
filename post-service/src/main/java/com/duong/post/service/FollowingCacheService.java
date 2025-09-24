package com.duong.post.service;

import com.duong.post.dto.response.UserFollowingResponse;
import com.duong.post.repository.http.ProfileClient;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowingCacheService {
    private final ProfileClient profileClient;

    @Cacheable(cacheNames = "following:by-me", key = "#userId", unless = "#result == null")
    public List<String> getFollowingIds(String userId) {
        try {
            var res = profileClient.getFollowings(userId);
            if (res == null || res.getResult() == null) return Collections.emptyList();
            return res.getResult().stream().map(UserFollowingResponse::getUserId).toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @CacheEvict(cacheNames = "following:by-me", key = "#userId")
    public void evictFollowing(String userId) {
        // gọi khi có sự kiện FOLLOW/UNFOLLOW để làm mới cache
    }
}
