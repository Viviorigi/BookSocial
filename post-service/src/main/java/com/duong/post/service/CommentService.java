package com.duong.post.service;

import com.duong.event.dto.NotificationEvent;
import com.duong.post.Helper.AfterCommit;
import com.duong.post.dto.response.PageResponse;
import com.duong.post.dto.request.CommentRequest;
import com.duong.post.dto.response.CommentResponse;
import com.duong.post.entity.Comment;
import com.duong.post.exception.AppException;
import com.duong.post.exception.ErrorCode;
import com.duong.post.messaging.NotificationProducer;
import com.duong.post.repository.CommentRepository;
import com.duong.post.repository.PostRepository;
import com.duong.post.repository.http.ProfileClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final DateTimeFormatter dateTimeFormatter;
    private final ProfileClient profileClient;
    private final NotificationProducer notificationProducer;

    public CommentResponse add(String postId, CommentRequest req) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();

        // verify post tồn tại & lấy chủ post
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));
        final String postOwnerId = post.getUserId();

        Comment c = Comment.builder()
                .postId(postId)
                .userId(me)
                .content(req.getContent())
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .build();
        c = commentRepository.save(c);
        postRepository.incrementCommentCount(postId, 1);

        // Sau commit mới gửi notif (và không tự gửi cho mình)
        if (postOwnerId != null && !postOwnerId.equals(me)) {
            final String actorId = me;
            final String commentId = c.getId();

            AfterCommit.run(() -> notificationProducer.send(NotificationEvent.builder()
                    .channel("IN_APP")
                    .recipient(postOwnerId)
                    .templateCode("COMMENT")
                    .param(Map.of(
                            "postId", postId,
                            "commentId", commentId,
                            "actorId", actorId
                    ))
                    .subject("Có bình luận mới")
                    .body("Người dùng " + actorId + " đã bình luận bài viết của bạn")
                    .build()));
        }



        String username = null;
        try {
            var p = profileClient.getProfile(me).getResult();
            username = p != null ? p.getUsername() : null;
        } catch (Exception ignored) {}

        return CommentResponse.builder()
                .id(c.getId())
                .postId(c.getPostId())
                .userId(c.getUserId())
                .username(username)
                .content(c.getContent())
                .created(dateTimeFormatter.format(c.getCreatedDate()))
                .createdDate(c.getCreatedDate())
                .build();
    }

    public PageResponse<CommentResponse> list(String postId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        var pageData = commentRepository.findAllByPostIdAndDeletedFalse(postId, pageable);

        // (đơn giản) N+1 lấy username từng comment — phase sau tối ưu batch
        var data = pageData.getContent().stream().map(c -> {
            String username = null;
            try { var p = profileClient.getProfile(c.getUserId()).getResult(); username = p!=null? p.getUsername():null; } catch (Exception ignored) {}
            return CommentResponse.builder()
                    .id(c.getId()).postId(c.getPostId()).userId(c.getUserId())
                    .username(username).content(c.getContent())
                    .created(dateTimeFormatter.format(c.getCreatedDate()))
                    .createdDate(c.getCreatedDate())
                    .build();
        }).toList();

        return PageResponse.<CommentResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(data)
                .build();
    }

    @Transactional
    public void deleteMyComment(String postId, String commentId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();

        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        // đảm bảo comment thuộc đúng post trên URL (chống lạm dụng id)
        if (!Objects.equals(c.getPostId(), postId)) {
            throw new AppException(ErrorCode.INVALID_KEY);
        }

        if (!Objects.equals(c.getUserId(), me)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!c.isDeleted()) {
            c.setDeleted(true);
            c.setModifiedDate(Instant.now());
            commentRepository.save(c);
            postRepository.incrementCommentCount(postId, -1);
        }
    }

    public long count(String postId) {
        return commentRepository.countByPostIdAndDeletedFalse(postId);
    }
}
