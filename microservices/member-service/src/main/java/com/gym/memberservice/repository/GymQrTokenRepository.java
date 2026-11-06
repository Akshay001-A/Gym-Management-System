package com.gym.memberservice.repository;

import com.gym.memberservice.entity.GymQrToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymQrTokenRepository extends JpaRepository<GymQrToken, Long> {
    Optional<GymQrToken> findByTokenHash(String tokenHash);
    long deleteByExpiresAtBefore(Instant instant);
}
