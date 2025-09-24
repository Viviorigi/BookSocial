package com.duong.post.repository;

import com.duong.post.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostRepositoryCustomImpl implements PostRepositoryCustom {
    private final MongoTemplate mongo;

    @Override
    public void incrementLikeCount(String postId, long delta) {
        Query q = new Query(Criteria.where("_id").is(postId));
        Update u = new Update().inc("likeCount", delta); // field trong Post.java là likeCount
        mongo.updateFirst(q, u, Post.class);
    }

    @Override
    public void incrementCommentCount(String postId, long delta) {
        Query q = new Query(Criteria.where("_id").is(postId));
        Update u = new Update().inc("commentCount", delta);
        mongo.updateFirst(q, u, Post.class);
    }
}
