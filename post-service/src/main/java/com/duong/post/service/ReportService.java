package com.duong.post.service;

import com.duong.post.dto.response.PostResponse;
import com.duong.post.dto.response.ReportWithPostResponse;
import com.duong.post.entity.Post;
import com.duong.post.entity.Report;
import com.duong.post.entity.Report.ReportStatus;
import com.duong.post.exception.AppException;
import com.duong.post.exception.ErrorCode;
import com.duong.post.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final MongoTemplate mongoTemplate; // dùng nếu cần update custom

    // User gửi report
    public Report createReport(String reporterId, String postId, String reason) {
        var report = Report.builder()
                .postId(postId)
                .reporterId(reporterId)
                .reason(reason)
                .status(ReportStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        return reportRepository.save(report);
    }

    // Admin xem danh sách report theo status
    public Page<ReportWithPostResponse> getReportsWithPost(ReportStatus status, Pageable pageable) {
        Page<Report> reports = reportRepository.findByStatus(status, pageable);

        return reports.map(report -> {
            // lấy post từ Mongo
            Post post = mongoTemplate.findById(report.getPostId(), Post.class, "post");

            PostResponse postResponse = null;
            if (post != null) {
                postResponse = PostResponse.builder()
                        .id(post.getId())
                        .content(post.getContent())
                        .userId(post.getUserId())
                        .createdDate(post.getCreatedDate())
                        .modifiedDate(post.getModifiedDate())
                        .likeCount(post.getLikeCount())
                        .commentCount(post.getCommentCount())
                        .build();
            }

            return ReportWithPostResponse.builder()
                    .id(report.getId())
                    .postId(report.getPostId())
                    .reporterId(report.getReporterId())
                    .reason(report.getReason())
                    .status(report.getStatus().name())
                    .createdAt(report.getCreatedAt())
                    .post(postResponse)
                    .build();
        });
    }

    // Admin xử lý report
    public Report handleReport(String reportId, ReportStatus status) {
        var report = reportRepository.findById(reportId)
                .orElseThrow(() -> new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION));

        report.setStatus(status);
        reportRepository.save(report);

        // Nếu APPROVE thì set deleted=true cho Post
        if (status == ReportStatus.APPROVED) {
            Query query = new Query(Criteria.where("_id").is(report.getPostId()));
            Update update = new Update().set("deleted", true);
            mongoTemplate.updateFirst(query, update, Post.class, "post");
        }

        return report;
    }
}
