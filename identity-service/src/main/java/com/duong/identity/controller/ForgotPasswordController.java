package com.duong.identity.controller;

import com.duong.identity.dto.request.ApiResponse;
import com.duong.identity.dto.request.ForgotPasswordRequest;
import com.duong.identity.dto.request.ResetPasswordRequest;
import com.duong.identity.service.PasswordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ForgotPasswordController {
    PasswordService passwordService;

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgot(@RequestBody @Valid ForgotPasswordRequest request) {
        passwordService.forgotPassword(request.email());
        return ApiResponse.<Void>builder().message("Reset link sent to email").build();
    }

    // API cho mobile/app (không dùng form HTML)
    @PostMapping("/reset-password-api")
    public ApiResponse<Void> reset(@RequestBody @Valid ResetPasswordRequest request) {
        passwordService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.<Void>builder().message("Password has been reset").build();
    }
}
