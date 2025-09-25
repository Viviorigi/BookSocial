package com.duong.post.controller;

import com.duong.post.dto.ApiResponse;
import com.duong.post.dto.response.PageResponse;
import com.duong.post.dto.request.PostRequest;
import com.duong.post.dto.response.PostResponse;
import com.duong.post.service.PostService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{postId}")
    public ApiResponse<String> delete(@PathVariable String postId) {
        postService.deleteMyPost(postId);
        return ApiResponse.<String>builder().result("Post deleted").build();
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

    @PostMapping("/{postId}/like")
    public ApiResponse<String> like(@PathVariable String postId) {
        postService.like(postId);
        return ApiResponse.<String>builder()
                .result("Liked successfully")
                .build();
    }

    @DeleteMapping("/{postId}/like")
    public ApiResponse<String> unlike(@PathVariable String postId) {
        postService.unlike(postId);
        return ApiResponse.<String>builder()
                .result("Unliked successfully")
                .build();
    }

    @GetMapping("/{postId}/likes/count")
    public ApiResponse<?> likeCount(@PathVariable String postId) {
        long count = postService.getLikeCount(postId);
        return ApiResponse.builder()
                .result( count)
                .build();
    }

    @GetMapping("/{postId}/likes/me")
    public ApiResponse<String> likedByMe(@PathVariable String postId) {
        boolean liked = postService.likedByMe(postId);
        return ApiResponse.<String>builder()
                .result(liked ? "You already liked this post" : "You have not liked this post")
                .build();
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<PostResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ApiResponse.<PageResponse<PostResponse>>builder()
                .result(postService.searchPosts(q, page, size))
                .build();
    }


}