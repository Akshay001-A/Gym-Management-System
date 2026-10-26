
package com.gym.membership.repository;

import com.gym.membership.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository
        extends JpaRepository<Membership, Long> {
}
