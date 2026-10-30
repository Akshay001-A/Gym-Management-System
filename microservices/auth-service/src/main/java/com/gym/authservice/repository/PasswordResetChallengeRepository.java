package com.gym.authservice.repository;

import com.gym.authservice.entity.PasswordResetChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetChallengeRepository extends JpaRepository<PasswordResetChallenge, String> { }
