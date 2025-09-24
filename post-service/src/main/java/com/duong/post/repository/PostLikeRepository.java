package com.duong.post.repository;

import com.duong.post.entity.PostLike;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PostLikeRepository extends MongoRepository<PostLike, String> {
    Optional<PostLike> findByPostIdAndUserId(String postId, String userId);
    boolean existsByPostIdAndUserId(String postId, String userId);
    long countByPostId(String postId);
    void deleteByPostIdAndUserId(String postId, String userId);
    void deleteAllByPostId(String postId);
}
