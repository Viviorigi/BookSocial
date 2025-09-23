package com.duong.profile.service;

import com.duong.profile.dto.request.ProfileCreationRequest;
import com.duong.profile.dto.request.SearchUserRequest;
import com.duong.profile.dto.request.UpdateProfileRequest;
import com.duong.profile.dto.response.SimpleUserDtoResponse;
import com.duong.profile.dto.response.UserProfileResponse;
import com.duong.profile.entity.UserProfile;
import com.duong.profile.exception.AppException;
import com.duong.profile.exception.ErrorCode;
import com.duong.profile.mapper.UserProfileMapper;
import com.duong.profile.repository.UserProfileRepository;
import com.duong.profile.repository.http.FileClient;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.AccessLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional(readOnly = true, transactionManager = "transactionManager")
public class UserProfileService {

    UserProfileRepository userProfileRepository;
    UserProfileMapper userProfileMapper;
    FileClient fileClient;

    // ===== Helper =====
    private String currentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    // ===== Create =====
    @Transactional(transactionManager = "transactionManager")
    public UserProfileResponse createProfile(ProfileCreationRequest request) {
        UserProfile saved = userProfileRepository.save(userProfileMapper.toUserProfile(request));
        // trả về profile có quan hệ chính xác
        return getByUserId(saved.getUserId());
    }

    // ===== Get (internal id -> chuyển sang userId để load graph) =====
    public UserProfileResponse getUserProfile(String id) {
        UserProfile p = userProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return getByUserId(p.getUserId());
    }

    // ===== Get all (nhẹ, không load graph) =====
    public List<UserProfileResponse> getAllProfiles() {
        return userProfileRepository.findAll()
                .stream().map(userProfileMapper::toUserProfileResponse).toList();
    }

    // ===== Get by userId (có graph) =====
    public UserProfileResponse getByUserId(String userId) {
        // 1) Lấy node chính (nhẹ)
        UserProfile node = userProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2) Map phần core
        UserProfileResponse res = userProfileMapper.toUserProfileResponse(node);

        // 3) Lấy count & list từ DB (chính xác 100%)
        res.setFollowersCount((int) userProfileRepository.countFollowers(userId));
        res.setFollowingCount((int) userProfileRepository.countFollowing(userId));

        // nếu muốn trả kèm một ít list (ví dụ top 20)
        var followers = userProfileRepository.findFollowersPage(userId, 0, 20);
        var following = userProfileRepository.findFollowingPage(userId, 0, 20);
        res.setFollowers(userProfileMapper.toSimpleList(followers));
        res.setFollowing(userProfileMapper.toSimpleList(following));

        return res;
    }

    public UserProfileResponse getMyProfile() {
        return getByUserId(currentUserId());
    }

    @Transactional(transactionManager = "transactionManager")
    public UserProfileResponse updateMyProfile(UpdateProfileRequest request) {
        String me = currentUserId();
        UserProfile profile = userProfileRepository.findByUserId(me)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userProfileMapper.update(profile, request);
        userProfileRepository.save(profile);
        return getByUserId(me);
    }

    @Transactional(transactionManager = "transactionManager")
    public UserProfileResponse updateAvatar(MultipartFile file) {
        String me = currentUserId();
        UserProfile profile = userProfileRepository.findByUserId(me)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        var uploaded = fileClient.uploadMedia(file);
        profile.setAvatar(uploaded.getResult().getUrl());
        userProfileRepository.save(profile);
        return getByUserId(me);
    }

    public List<UserProfileResponse> search(SearchUserRequest request) {
        String me = currentUserId();
        return userProfileRepository.findAllByUsernameLike(request.getKeyword())
                .stream()
                .filter(u -> !me.equals(u.getUserId()))
                .map(userProfileMapper::toUserProfileResponse)
                .toList();
    }

    // ===== FOLLOW / UNFOLLOW =====
    @Transactional(transactionManager = "transactionManager")
    public void follow(String targetUserId) {
        String me = currentUserId();
        if (me.equals(targetUserId)) throw new AppException(ErrorCode.INVALID_KEY); // không tự follow
        userProfileRepository.findByUserId(me)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        userProfileRepository.follow(me, targetUserId); // idempotent
    }

    @Transactional(transactionManager = "transactionManager")
    public void unfollow(String targetUserId) {
        String me = currentUserId();
        if (me.equals(targetUserId)) return; // no-op
        userProfileRepository.unfollow(me, targetUserId); // idempotent
    }

    public List<SimpleUserDtoResponse> getFollowing(String userId, int page, int size) {
        long skip = (long) Math.max(page, 0) * Math.max(size, 1);
        return userProfileMapper.toSimpleList(userProfileRepository.findFollowingPage(userId, skip, size));
    }

    public List<SimpleUserDtoResponse> getFollowers(String userId, int page, int size) {
        long skip = (long) Math.max(page, 0) * Math.max(size, 1);
        return userProfileMapper.toSimpleList(userProfileRepository.findFollowersPage(userId, skip, size));
    }

    public long countFollowing(String userId) { return userProfileRepository.countFollowing(userId); }
    public long countFollowers(String userId) { return userProfileRepository.countFollowers(userId); }
}
