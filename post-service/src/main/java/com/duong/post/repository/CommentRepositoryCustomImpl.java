package com.duong.post.repository;

import com.duong.post.entity.Comment;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentRepositoryCustomImpl implements CommentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void softDeleteByPostId(String postId) {
        Query q = new Query(Criteria.where("postId").is(postId));
        Update u = new Update().set("deleted", true).currentDate("modifiedDate");
        mongoTemplate.updateMulti(q, u, Comment.class);
    }
}