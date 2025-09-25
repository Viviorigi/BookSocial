package com.duong.post.controller;

import com.duong.post.dto.ApiResponse;
import com.duong.post.dto.response.PageResponse;
import com.duong.post.dto.request.CommentRequest;
import com.duong.post.dto.response.CommentResponse;
import com.duong.post.service.CommentService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentController {
    CommentService commentService;

    @PostMapping("/{postId}/comments")
    public ApiResponse<CommentResponse> add(@PathVariable String postId,
                                            @Valid @RequestBody CommentRequest req) {
        return ApiResponse.<CommentResponse>builder()
                .result(commentService.add(postId, req))
                .build();
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<PageResponse<CommentResponse>> list(@PathVariable String postId,
                                                           @RequestParam(defaultValue="1") int page,
                                                           @RequestParam(defaultValue="10") int size) {
        return ApiResponse.<PageResponse<CommentResponse>>builder()
                .result(commentService.list(postId, page, size))
                .build();
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    public ApiResponse<String> delete(@PathVariable String postId,
                                      @PathVariable String commentId) {
        commentService.deleteMyComment(postId,commentId);
        return ApiResponse.<String>builder().result("Deleted").build();
    }

    @GetMapping("/{postId}/comments/count")
    public ApiResponse<Long> count(@PathVariable String postId) {
        return ApiResponse.<Long>builder().result(commentService.count(postId)).build();
    }
}
