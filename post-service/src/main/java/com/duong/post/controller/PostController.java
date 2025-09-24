package com.duong.post.controller;

import com.duong.post.dto.ApiResponse;
import com.duong.post.dto.PageResponse;
import com.duong.post.dto.request.PostRequest;
import com.duong.post.dto.response.PostResponse;
import com.duong.post.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostController {
    PostService postService;

    @PostMapping("/create")
    ApiResponse<PostResponse> createPost(@RequestBody PostRequest postRequest) {
        return ApiResponse.<PostResponse>builder()
                .result(postService.createPost(postRequest))
                .build();
    }

    @GetMapping("/my-posts")
    ApiResponse<PageResponse<PostResponse>> getMyPosts(
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size

            ) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getMyPosts(page, size))
                .build();
    }

    @GetMapping("/users/{userId}")
    ApiResponse<PageResponse<PostResponse>> getUserPosts(
            @PathVariable String userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getUserPosts(userId, page, size))
                .build();
    }

    @GetMapping("/feed")
    ApiResponse<PageResponse<PostResponse>> getFeed(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.getFeed(page, size))
                .build();
    }
}