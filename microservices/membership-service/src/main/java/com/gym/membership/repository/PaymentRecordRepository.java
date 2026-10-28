package com.gym.membership.repository;

import com.gym.membership.entity.PaymentRecord;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);
    boolean existsByMembershipId(Long membershipId);
    Optional<PaymentRecord> findByIdAndMemberId(Long id, Long memberId);
    Page<PaymentRecord> findByMemberId(Long memberId, Pageable pageable);
}
