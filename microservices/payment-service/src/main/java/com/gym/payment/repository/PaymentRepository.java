
package com.gym.payment.repository;

import com.gym.payment.entity.Payment;
import com.gym.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByIdAndMemberId(Long id, Long memberId);

    Page<Payment> findByMemberIdOrderByCreatedAtDesc(
            Long memberId,
            Pageable pageable
    );

    Optional<Payment> findFirstByMemberIdAndPlanNameAndStatusAndVerifiedAtNotNullOrderByVerifiedAtDesc(
            Long memberId,
            String planName,
            PaymentStatus status
    );

    boolean existsByRazorpayPaymentIdAndStatus(
            String razorpayPaymentId,
            PaymentStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.razorpayOrderId = :orderId")
    Optional<Payment> findByRazorpayOrderIdForUpdate(
            @Param("orderId") String orderId
    );
}
