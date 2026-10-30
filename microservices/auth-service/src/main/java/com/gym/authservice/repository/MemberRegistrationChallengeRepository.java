package com.gym.authservice.repository;

import com.gym.authservice.entity.MemberRegistrationChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRegistrationChallengeRepository extends JpaRepository<MemberRegistrationChallenge, String> {
}
