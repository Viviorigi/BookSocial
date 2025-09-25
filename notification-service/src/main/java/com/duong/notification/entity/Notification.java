package com.duong.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document("notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(name = "user_created_idx", def = "{'userId':1,'createdAt':-1}"),
        @CompoundIndex(name = "user_read_idx",    def = "{'userId':1,'read':1}")
})
public class Notification {
    @Id
    private String id;

    private String userId;        // Ai nhận
    private String type;          // FOLLOW, LIKE, COMMENT, SYSTEM...
    private String message;       // Nội dung ngắn
    private boolean read;         // Đã đọc chưa
    private Instant createdAt;    // Thời điểm tạo
    private Map<String, Object> metadata; // dữ liệu thêm (postId, actorId...)
}
