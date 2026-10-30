package com.gym.authservice.repository;

import com.gym.authservice.entity.EmailChangeChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailChangeChallengeRepository extends JpaRepository<EmailChangeChallenge, Long> { }
