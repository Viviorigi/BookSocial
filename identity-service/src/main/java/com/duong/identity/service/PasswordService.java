package com.duong.identity.service;

import com.duong.event.dto.NotificationEvent;
import com.duong.identity.entity.PasswordResetToken;
import com.duong.identity.entity.User;
import com.duong.identity.exception.AppException;
import com.duong.identity.exception.ErrorCode;
import com.duong.identity.repository.PasswordResetTokenRepository;
import com.duong.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        log.info("Forgot password id {}", user.getId());

        // Xoá token cũ (nếu có)
        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .userId(user.getId())
                .expiryDate(Instant.now().plus(15, ChronoUnit.MINUTES))
                .build();

        tokenRepository.save(resetToken);

        // Gửi email qua Notification Service (Kafka)
        NotificationEvent event = NotificationEvent.builder()
                .channel("EMAIL")
                .recipient(user.getEmail())
                .subject("Reset your password")
                .body("Token Reset " + token)
                .build();
        kafkaTemplate.send("notification-delivery", event);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken prt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_TOKEN));

        if (prt.isUsed()) {
            throw new AppException(ErrorCode.TOKEN_USED);
        }
        if (prt.getExpiryDate().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }

        User user = userRepository.findById(prt.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new AppException(ErrorCode.PASSWORD_SAME_AS_OLD);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        prt.setUsed(true);
        tokenRepository.save(prt);
    }
}
