package com.duong.notification.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class NotificationOut {
    private String id;
    private String type;        // COMMENT / LIKE / FOLLOW...
    private String message;     // nội dung ngắn: "Có bình luận mới"
    private Instant createdAt;

    // Metadata tối thiểu để FE điều hướng
    private String actorId;
    private String postId;
    private String commentId;
}
