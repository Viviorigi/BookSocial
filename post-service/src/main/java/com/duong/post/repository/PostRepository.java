package com.duong.post.repository;

import com.duong.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends MongoRepository<Post, String>, PostRepositoryCustom {
    @Query("{ 'userId': ?0, 'deleted': { $ne: true } }")
    Page<Post> findActiveByUserId(String userId, Pageable pageable);

    @Query("{ 'userId': { $in: ?0 }, 'deleted': { $ne: true } }")
    Page<Post> findActiveByUserIdIn(Collection<String> userIds, Pageable pageable);

    Optional<Post> findByIdAndDeletedFalse(String id);

}
