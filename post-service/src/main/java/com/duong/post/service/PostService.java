package com.duong.post.service;

import com.duong.post.dto.PageResponse;
import com.duong.post.dto.request.CommentRequest;
import com.duong.post.dto.request.PostRequest;
import com.duong.post.dto.response.CommentResponse;
import com.duong.post.dto.response.PostResponse;
import com.duong.post.dto.response.UserFollowingResponse;
import com.duong.post.dto.response.UserProfileResponse;
import com.duong.post.entity.Comment;
import com.duong.post.entity.Post;
import com.duong.post.entity.PostLike;
import com.duong.post.exception.AppException;
import com.duong.post.exception.ErrorCode;
import com.duong.post.mapper.PostMapper;
import com.duong.post.repository.CommentRepository;
import com.duong.post.repository.PostLikeRepository;
import com.duong.post.repository.PostRepository;
import com.duong.post.repository.http.ProfileClient;
import com.mongodb.DuplicateKeyException;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PostService {
    PostRepository postRepository;
    PostLikeRepository postLikeRepository;
    PostMapper postMapper;
    DateTimeFormatter dateTimeFormatter;
    ProfileClient profileClient;
    CommentRepository commentRepository;
    ProfileCacheService profileCacheService;
    FollowingCacheService followingCacheService;

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

    @Transactional
    public void deleteMyPost(String postId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_KEY));

        if (!Objects.equals(post.getUserId(), me)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!post.isDeleted()) {
            post.setDeleted(true);
            post.setModifiedDate(Instant.now());
            postRepository.save(post);

            // (optional) xóa mềm luôn comments & likes liên quan
            commentRepository.softDeleteByPostId(postId);

            // Xóa hết like (xóa cứng luôn)
            postLikeRepository.deleteAllByPostId(postId);
        }
    }

    public PageResponse<PostResponse> getMyPosts(int page, int size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId= authentication.getName();

        UserProfileResponse userProfile = null;
        try {
            userProfile = profileCacheService.getProfile(userId);
        } catch (Exception e) {
            log.error("Error while getting user profile", e);
        }

        Sort sort = Sort.by("createdDate").descending();
        Pageable pageable = PageRequest.of(page - 1, size,sort);

        var pageData = postRepository.findActiveByUserId(userId, pageable);

        String username = userProfile != null ? userProfile.getUsername() : null;
        var postList = pageData.getContent().stream().map(post -> {
            var postResponse = postMapper.toPostResponse(post);
            postResponse.setCreated(dateTimeFormatter.format(post.getCreatedDate()));
            postResponse.setUsername(username);
            postResponse.setLikeCount(post.getLikeCount());
            postResponse.setLikedByMe(postLikeRepository
                    .existsByPostIdAndUserId(post.getId(), userId));
            postResponse.setCommentCount(post.getCommentCount());
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
        String me = SecurityContextHolder.getContext().getAuthentication().getName();
        // lấy profile của người được xem
        UserProfileResponse profile = null;
        try {
            profile = profileClient.getProfile(userId).getResult();
        } catch (Exception e) {
            log.warn("Get profile failed for userId={}", userId, e);
        }

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        var pageData = postRepository.findActiveByUserId(userId, pageable);

        String username = profile != null ? profile.getUsername() : null;

        List<PostResponse> data = pageData.getContent().stream().map(p -> {
            PostResponse dto = postMapper.toPostResponse(p);
            dto.setCreated(dateTimeFormatter.format(p.getCreatedDate()));
            dto.setUsername(username);
            dto.setLikeCount(p.getLikeCount());                 // lấy từ Post
            dto.setLikedByMe(postLikeRepository
                    .existsByPostIdAndUserId(p.getId(),me ));
            dto.setCommentCount(p.getCommentCount());
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

        List<String> followingIds = followingCacheService.getFollowingIds(me);

        // scope = chính mình + những người mình follow
        var scope = new java.util.HashSet<String>();
        scope.add(me);
        if (followingIds != null) scope.addAll(followingIds);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        var pageData = postRepository.findActiveByUserIdIn(scope, pageable);

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
            dto.setLikeCount(p.getLikeCount());                 // lấy từ Post
            dto.setLikedByMe(postLikeRepository
                    .existsByPostIdAndUserId(p.getId(), me));
            dto.setCommentCount(p.getCommentCount());
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

    public PageResponse<PostResponse> searchPosts(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdDate").descending());
        var pageData = postRepository.searchByContent(keyword, pageable);

        List<PostResponse> data = pageData.getContent().stream().map(p -> {
            String username = null;
            try {
                var profile = profileClient.getProfile(p.getUserId()).getResult();
                username = profile != null ? profile.getUsername() : null;
            } catch (Exception ignored) {}

            PostResponse dto = postMapper.toPostResponse(p);
            dto.setCreated(dateTimeFormatter.format(p.getCreatedDate()));
            dto.setUsername(username);
            dto.setLikeCount(p.getLikeCount());
            dto.setLikedByMe(postLikeRepository.existsByPostIdAndUserId(p.getId(),
                    SecurityContextHolder.getContext().getAuthentication().getName()));
            dto.setCommentCount(p.getCommentCount());
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

    @Transactional
    public void like(String postId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();

        if (postLikeRepository.existsByPostIdAndUserId(postId, me)) return;

        try {
            postLikeRepository.save(PostLike.builder()
                    .postId(postId)
                    .userId(me)
                    .createdAt(Instant.now())
                    .build());
            postRepository.incrementLikeCount(postId, 1);
        } catch (DuplicateKeyException e) {

            log.debug("Duplicate like postId={}, userId={}", postId, me);
        }
    }

    @Transactional
    public void unlike(String postId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();

        if (!postLikeRepository.existsByPostIdAndUserId(postId, me)) return;

        postLikeRepository.deleteByPostIdAndUserId(postId, me);
        postRepository.incrementLikeCount(postId, -1);
    }

    public long getLikeCount(String postId) {

        return postRepository.findById(postId).map(Post::getLikeCount).orElse(0L);
    }

    public boolean likedByMe(String postId) {
        String me = SecurityContextHolder.getContext().getAuthentication().getName();
        return postLikeRepository.existsByPostIdAndUserId(postId, me);
    }

    // Gợi ý: khi build PageResponse cho feed/my-posts, set thêm likedByMe
//    private void enrichLikedFlag(List<PostResponse> list) {
//        String me = SecurityContextHolder.getContext().getAuthentication().getName();
//        // batch check: gom các postId rồi query existence theo từng cái (ở Mongo khó 1 phát)
//        // tạm thời check từng cái (đủ dùng cho demo; tối ưu sau bằng map cache)
//        for (var dto : list) {
//            if (dto.getId() != null) {
//                dto.setLikedByMe(postLikeRepository.existsByPostIdAndUserId(dto.getId(), me));
//            }
//        }
//    }

}
