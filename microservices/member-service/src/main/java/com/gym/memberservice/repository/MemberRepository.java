
package com.gym.memberservice.repository;

import com.gym.memberservice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByMobile(String mobile);
    Optional<Member> findByMobile(String mobile);

    // Find a member using the email from the JWT.
    Optional<Member> findByEmailIgnoreCase(String email);

    Optional<Member> findByRegistrationKey(String registrationKey);
}
