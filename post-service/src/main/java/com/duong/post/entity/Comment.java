package com.duong.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("post_comments")
@FieldDefaults(level = AccessLevel.PRIVATE)
@CompoundIndexes({
        @CompoundIndex(name="post_created_idx", def="{'postId':1,'createdDate':-1}")
})
public class Comment {
    @MongoId
    String id;
    @Indexed
    String postId;
    @Indexed
    String userId;
    String content;
    Instant createdDate;
    Instant modifiedDate;
    @Builder.Default boolean deleted = false;
}
