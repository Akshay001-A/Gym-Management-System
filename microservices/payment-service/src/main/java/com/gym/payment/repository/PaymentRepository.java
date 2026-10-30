
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
import java.time.Instant;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT COALESCE(SUM(p.amountPaise), 0) FROM Payment p WHERE p.status = :status AND p.verifiedAt >= :start AND p.verifiedAt < :end")
    Long sumSuccessfulAmountBetween(@Param("status") PaymentStatus status,
            @Param("start") Instant start, @Param("end") Instant end);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status AND p.verifiedAt >= :start AND p.verifiedAt < :end")
    Long countSuccessfulPaymentsBetween(@Param("status") PaymentStatus status,
            @Param("start") Instant start, @Param("end") Instant end);

    org.springframework.data.domain.Page<Payment> findByStatusAndVerifiedAtGreaterThanEqualAndVerifiedAtLessThanOrderByVerifiedAtDesc(
            PaymentStatus status, Instant start, Instant end, org.springframework.data.domain.Pageable pageable);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);

    Optional<Payment> findByIdAndMemberId(Long id, Long memberId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :paymentId")
    Optional<Payment> findByIdForUpdate(@Param("paymentId") Long paymentId);

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
