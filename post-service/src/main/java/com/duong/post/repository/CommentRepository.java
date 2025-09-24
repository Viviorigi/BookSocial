package com.duong.post.repository;

import com.duong.post.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface CommentRepository extends MongoRepository<Comment, String>,CommentRepositoryCustom  {
    Page<Comment> findAllByPostIdAndDeletedFalse(String postId, Pageable pageable);
    long countByPostIdAndDeletedFalse(String postId);


}