package com.duong.identity.controller;

import java.util.List;
import java.util.Map;

import com.duong.identity.repository.UserRepository;
import com.duong.identity.service.EmailVerificationService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.duong.identity.dto.request.ApiResponse;
import com.duong.identity.dto.request.UserCreationRequest;
import com.duong.identity.dto.request.UserUpdateRequest;
import com.duong.identity.dto.response.UserResponse;
import com.duong.identity.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserController {
    UserService userService;
    private final UserRepository userRepo;
    EmailVerificationService emailVerificationService;

    @PostMapping("/registration")
    ApiResponse<UserResponse> createUser(@RequestBody @Valid UserCreationRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.createUser(request))
                .build();
    }

    @GetMapping
    ApiResponse<List<UserResponse>> getUsers() {
        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/{userId}")
    ApiResponse<UserResponse> getUser(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @GetMapping("/my-info")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @DeleteMapping("/{userId}")
    ApiResponse<String> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder().result("User has been deleted").build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserResponse> updateUser(@PathVariable String userId, @RequestBody UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUser(userId, request))
                .build();
    }

    @PostMapping("/verify-token")
    public ApiResponse<?> verify(@RequestBody Map<String,String> body) {
        emailVerificationService.verify(
                body.get("token"),
                userId -> userRepo.findById(userId).orElse(null),
                userRepo::save
        );
        return ApiResponse.builder().result("Email Verify Successful").build();
    }

    @PostMapping("/resend-verification")
    public ApiResponse<String> resendVerification(@RequestBody Map<String, String> body) {
        userService.resendEmailVerification(body.get("email"));
        return ApiResponse.<String>builder()
                .message("Verification token sent")
                .result("OK")
                .build();
    }
}
