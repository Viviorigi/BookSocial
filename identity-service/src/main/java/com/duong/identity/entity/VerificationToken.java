package com.duong.identity.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "verification_tokens",
        indexes = {
                @Index(name = "idx_vt_token_unique", columnList = "token", unique = true),
                @Index(name = "idx_vt_user_type", columnList = "user_id,type")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VerificationToken {

    public enum Type { EMAIL_VERIFY, PASSWORD_RESET }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // null = chưa dùng; có giá trị = đã dùng (idempotent)
    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
