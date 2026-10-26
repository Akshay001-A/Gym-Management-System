
package com.gym.memberservice.repository;

import com.gym.memberservice.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);
}
