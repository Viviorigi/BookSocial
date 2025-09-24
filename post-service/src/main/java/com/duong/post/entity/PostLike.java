package com.duong.post.entity;


import lombok.*;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Document("post_likes")
@Data
@Builder @NoArgsConstructor @AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "uniq_post_user", def = "{'postId': 1, 'userId': 1}", unique = true)
})
public class PostLike {
    @MongoId
    String id;
    @Indexed
    String postId;
    @Indexed
    String userId;
    Instant createdAt;
}
