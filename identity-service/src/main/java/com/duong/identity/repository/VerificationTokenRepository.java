package com.duong.identity.repository;

import com.duong.identity.entity.VerificationToken;
import com.duong.identity.entity.VerificationToken.Type;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {

    Optional<VerificationToken> findByTokenAndTypeAndConsumedAtIsNull(String token, Type type);

    @Modifying
    @Query("delete from VerificationToken v where v.userId = :userId and v.type = :type")
    int deleteAllByUserIdAndType(@Param("userId") String userId, @Param("type") Type type);

    @Modifying
    @Query("update VerificationToken v set v.consumedAt = :consumedAt " +
            "where v.id = :id and v.consumedAt is null")
    int softConsume(@Param("id") String id, @Param("consumedAt") Instant consumedAt);

    boolean existsByToken(String token);
}

