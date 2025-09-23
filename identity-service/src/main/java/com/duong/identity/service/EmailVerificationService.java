package com.duong.identity.service;

import com.duong.event.dto.NotificationEvent;
import com.duong.identity.entity.User;
import com.duong.identity.entity.VerificationToken;
import com.duong.identity.exception.AppException;
import com.duong.identity.exception.ErrorCode;
import com.duong.identity.repository.VerificationTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private final VerificationTokenRepository tokenRepo;
    private final KafkaTemplate<String, NotificationEvent> kafkaTemplate;
    private final VerifyCooldownService cooldown;

    private static final Duration TTL = Duration.ofHours(24);
    private static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    @Transactional
    public String sendRawToken(String userId, String username, String email) {
        tokenRepo.deleteAllByUserIdAndType(userId, VerificationToken.Type.EMAIL_VERIFY);

        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepo.save(VerificationToken.builder()
                .userId(userId)
                .type(VerificationToken.Type.EMAIL_VERIFY)
                .token(token)
                .expiresAt(Instant.now().plus(Duration.ofHours(24)))
                .createdAt(Instant.now())
                .build());

        kafkaTemplate.send("notification-delivery",
                NotificationEvent.builder()
                        .channel("EMAIL")
                        .recipient(email)
                        .subject("Verify email")
                        .body("Hello " + username + ", your token: " + token)
                        .build());
        return token;
    }

    @Transactional
    public void verify(String token, Function<String, User> loadUserById, Consumer<User> saveUser) {
        var vt = tokenRepo.findByTokenAndTypeAndConsumedAtIsNull(token, VerificationToken.Type.EMAIL_VERIFY)
                .orElseThrow(() -> new RuntimeException("INVALID_TOKEN"));
        if (vt.getExpiresAt().isBefore(Instant.now())) throw new RuntimeException("TOKEN_EXPIRED");
        tokenRepo.softConsume(vt.getId(), Instant.now());

        User user = loadUserById.apply(vt.getUserId());
        if (user == null) throw new RuntimeException("USER_NOT_FOUND");

        user.setEmailVerified(true);
        saveUser.accept(user);
    }

    @Transactional
    public void resendVerifyToken(User user) {
        if (user.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_VERIFIED) ;
        }
        // chặn spam
        if (!cooldown.tryAcquire(user.getId(), RESEND_COOLDOWN)) {
            throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
        }

        // cấp token mới & gửi
        tokenRepo.deleteAllByUserIdAndType(user.getId(), VerificationToken.Type.EMAIL_VERIFY);
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenRepo.save(VerificationToken.builder()
                .userId(user.getId())
                .type(VerificationToken.Type.EMAIL_VERIFY)
                .token(token)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(TTL))
                .build());

        kafkaTemplate.send("notification-delivery", NotificationEvent.builder()
                .channel("EMAIL").recipient(user.getEmail())
                .subject("Verify email")
                .body("Hello " + user.getUsername() + ", your token: " + token)
                .build());
    }
}
