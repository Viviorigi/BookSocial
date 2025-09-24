package com.duong.post.repository;

public interface CommentRepositoryCustom {
    void softDeleteByPostId(String postId);
}
