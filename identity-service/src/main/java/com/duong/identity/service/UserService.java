package com.duong.identity.service;

import java.util.HashSet;
import java.util.List;

import com.duong.event.dto.NotificationEvent;
import com.duong.identity.constant.PredefinedRole;
import com.duong.identity.exception.AppException;
import com.duong.identity.exception.ErrorCode;
import com.duong.identity.mapper.ProfileMapper;
import com.duong.identity.mapper.UserMapper;
import com.duong.identity.repository.RoleRepository;
import com.duong.identity.repository.UserRepository;
import com.duong.identity.repository.httpclient.ProfileClient;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.duong.identity.dto.request.UserCreationRequest;
import com.duong.identity.dto.request.UserUpdateRequest;
import com.duong.identity.dto.response.UserResponse;
import com.duong.identity.entity.Role;
import com.duong.identity.entity.User;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {
    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    ProfileClient profileClient;
    ProfileMapper profileMapper;
    KafkaTemplate<String, Object> kafkaTemplate;
    EmailVerificationService emailVerificationService;

    public UserResponse createUser(UserCreationRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) throw new AppException(ErrorCode.USER_EXISTED);

        User user = userMapper.toUser(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.USER_ROLE).ifPresent(roles::add);

        user.setRoles(roles);

        user.setEmailVerified(false);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException exception){
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        var profileRequest = profileMapper.toProfileCreationRequest(request);
        profileRequest.setUserId(user.getId());

        profileClient.createProfile(profileRequest);

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(request.getEmail())
                .subject("Welcome to booksocial")
                .body("Hello, "+    request.getUsername())
                .build();
        // Public message to  kafka
        kafkaTemplate.send("notification-delivery", notificationEvent);

        emailVerificationService.sendRawToken(user.getId(), user.getUsername(), user.getEmail());

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String userid = context.getAuthentication().getName();

        User user = userRepository.findById(userid).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(String userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        userMapper.updateUser(user, request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        var roles = roleRepository.findAllById(request.getRoles());
        user.setRoles(new HashSet<>(roles));

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(String userId) {
        userRepository.deleteById(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsers() {
        log.info("In method get Users");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUser(String id) {
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
    }

    @Transactional
    public void resendEmailVerification(String email) {
        // 1. Tìm user theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        // 2. Nếu đã verify thì báo lỗi
        if (user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED);
        }

        // 3. Gọi service gửi lại token (có cooldown Redis bên trong)
        emailVerificationService.resendVerifyToken(user);
    }

}
