package com.duong.post.service;

import com.duong.post.dto.PageResponse;
import com.duong.post.dto.request.PostRequest;
import com.duong.post.dto.response.PostResponse;
import com.duong.post.dto.response.UserFollowingResponse;
import com.duong.post.dto.response.UserProfileResponse;
import com.duong.post.entity.Post;
import com.duong.post.mapper.PostMapper;
import com.duong.post.repository.PostRepository;
import com.duong.post.repository.http.ProfileClient;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {
    PostRepository postRepository;
    PostMapper postMapper;
    DateTimeFormatter dateTimeFormatter;
    ProfileClient profileClient;

    public PostResponse createPost(PostRequest postRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Post  post = Post.builder()
                .content(postRequest.getContent())
                .userId(authentication.getName())
                .createdDate(Instant.now())
                .modifiedDate(Instant.now())
                .build();

        post = postRepository.save(post);
        return  postMapper.toPostResponse(post);
    }

    public PageResponse<PostResponse> getMyPosts(int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId= authentication.getName();

        UserProfileResponse userProfile = null;
        try {
            userProfile = profileClient.getProfile(userId).getResult();
        } catch (Exception e) {
            log.error("Error while getting user profile", e);
        }

        Sort sort = Sort.by("createdDate").descending();
        Pageable pageable = PageRequest.of(page - 1, size,sort);

        var pageData = postRepository.findAllByUserId(userId, pageable);

        String username = userProfile != null ? userProfile.getUsername() : null;
        var postList = pageData.getContent().stream().map(post -> {
            var postResponse = postMapper.toPostResponse(post);
            postResponse.setCreated(dateTimeFormatter.format(post.getCreatedDate()));
            postResponse.setUsername(username);
            return postResponse;
        }).toList();
        
        return  PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(postList)
                .build();
    }

    public PageResponse<PostResponse> getUserPosts(String userId, int page, int size) {
        // lấy profile của người được xem
        UserProfileResponse profile = null;
        try {
            profile = profileClient.getProfile(userId).getResult();
        } catch (Exception e) {
            log.warn("Get profile failed for userId={}", userId, e);
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        var pageData = postRepository.findAllByUserId(userId, pageable);

        String username = profile != null ? profile.getUsername() : null;

        List<PostResponse> data = pageData.getContent().stream().map(p -> {
            PostResponse dto = postMapper.toPostResponse(p);
            dto.setCreated(dateTimeFormatter.format(p.getCreatedDate()));
            dto.setUsername(username);
            return dto;
        }).toList();

        return PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(data)
                .build();
    }

    public PageResponse<PostResponse> getFeed(int page, int size) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();

        List<String> followingIds = List.of();
        try {
            var res = profileClient.getFollowings(me);
            followingIds = res != null && res.getResult() != null
                    ? res.getResult().stream()
                    .map(UserFollowingResponse::getUserId)  // map sang id
                    .toList()
                    : List.of();
        } catch (Exception e) {
            log.warn("Get followings failed for {}", me, e);
        }

        // scope = chính mình + những người mình follow
        var scope = new java.util.HashSet<String>();
        scope.add(me);
        if (followingIds != null) scope.addAll(followingIds);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        var pageData = postRepository.findAllByUserIdIn(scope, pageable);

        // Đơn giản: gọi profile từng post (N+1) — tạm chấp nhận ở Phase 2 để chạy ngay
        // Phase sau sẽ tối ưu bằng batch + cache
        List<PostResponse> data = pageData.getContent().stream().map(p -> {
            String username = null;
            try {
                var profile = profileClient.getProfile(p.getUserId()).getResult();
                username = profile != null ? profile.getUsername() : null;
            } catch (Exception ignored) {}

            PostResponse dto = postMapper.toPostResponse(p);
            dto.setCreated(dateTimeFormatter.format(p.getCreatedDate()));
            dto.setUsername(username);
            return dto;
        }).toList();

        return PageResponse.<PostResponse>builder()
                .currentPage(page)
                .pageSize(pageData.getSize())
                .totalPages(pageData.getTotalPages())
                .totalElements(pageData.getTotalElements())
                .data(data)
                .build();
    }

}
