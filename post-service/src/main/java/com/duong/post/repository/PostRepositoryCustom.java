package com.duong.post.repository;

public interface PostRepositoryCustom {
    void incrementLikeCount(String postId, long delta);
    void incrementCommentCount(String postId, long delta);
}
