package com.duong.post.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Document(collection = "reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    @MongoId
    private String id;

    private String postId;
    private String reporterId;
    private String reason;

    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Builder.Default
    private Instant createdAt = Instant.now();

    public enum ReportStatus {
        PENDING,
        APPROVED,
        REJECTED
    }
}

