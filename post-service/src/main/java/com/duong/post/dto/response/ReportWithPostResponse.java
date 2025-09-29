package com.duong.post.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReportWithPostResponse {
    String id;          // reportId
    String postId;
    String reporterId;
    String reason;
    String status;
    Instant createdAt;

    private PostResponse post;
}
