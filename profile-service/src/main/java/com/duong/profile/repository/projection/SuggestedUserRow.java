package com.duong.profile.repository.projection;

public record SuggestedUserRow(
        String userId,
        String username,
        String avatar,
        Long mutuals
) {}
