package com.gym.membership.repository;

import com.gym.membership.entity.Membership;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByMemberId(Long memberId);
    java.util.Optional<Membership> findByPaymentId(Long paymentId);
    java.util.Optional<Membership> findFirstByMemberIdAndPlanNameAndStatusOrderByIdDesc(Long memberId, String planName, String status);
    java.util.Optional<Membership> findFirstByMemberIdAndStatusAndEndDateGreaterThanEqualOrderByEndDateDesc(
            Long memberId, String status, LocalDate today);
    Page<Membership> findByStatusIgnoreCase(String status, Pageable pageable);
    Page<Membership> findByMemberIdAndStatusIgnoreCase(Long memberId, String status, Pageable pageable);
    Page<Membership> findByMemberId(Long memberId, Pageable pageable);

    boolean existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long memberId, String status, LocalDate requestedEnd, LocalDate requestedStart);
    boolean existsByMemberIdAndStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualAndIdNot(
            Long memberId, String status, LocalDate requestedEnd, LocalDate requestedStart, Long excludedId);

    @Modifying
    @Query("update Membership m set m.status = 'EXPIRED' where m.status = 'ACTIVE' and m.endDate < :today")
    int expireActiveMemberships(@Param("today") LocalDate today);
}
