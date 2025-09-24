package com.duong.post.entity;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Getter
@Setter
@Builder
@Document(value = "post")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Post {
    @MongoId
    String id;
    @Indexed
    String userId;
    String content;

    @Builder.Default
    long likeCount = 0L;

    @Builder.Default
    long commentCount = 0L;

    Instant createdDate;
    Instant modifiedDate;

    @Builder.Default
    boolean deleted = false;

    @PersistenceCreator
    public Post(String id,
                String userId,
                String content,
                Long likeCount,
                Long commentCount,
                Instant createdDate,
                Instant modifiedDate,
                Boolean deleted) {
        this.id = id;
        this.userId = userId;
        this.content = content;
        this.likeCount = likeCount == null ? 0L : likeCount;
        this.commentCount = commentCount == null ? 0L : commentCount;
        this.createdDate = createdDate;
        this.modifiedDate = modifiedDate;
        this.deleted = deleted != null && deleted;
    }
}
