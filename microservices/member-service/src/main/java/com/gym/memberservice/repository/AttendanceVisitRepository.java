package com.gym.memberservice.repository;

import com.gym.memberservice.entity.AttendanceVisit;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceVisitRepository extends JpaRepository<AttendanceVisit, Long> {
    Optional<AttendanceVisit> findFirstByMemberIdAndCheckOutAtIsNullOrderByCheckInAtDesc(Long memberId);

    Optional<AttendanceVisit> findFirstByMemberIdOrderByCheckInAtDesc(Long memberId);

    @Query("select visit from AttendanceVisit visit where visit.memberId = :memberId "
            + "and visit.checkInAt < :rangeEnd "
            + "and (visit.checkOutAt is null or visit.checkOutAt > :rangeStart) "
            + "order by visit.checkInAt desc")
    List<AttendanceVisit> findOverlappingVisits(@Param("memberId") Long memberId,
            @Param("rangeStart") Instant rangeStart, @Param("rangeEnd") Instant rangeEnd);

    @Query("select visit from AttendanceVisit visit where visit.checkInAt < :rangeEnd "
            + "and (visit.checkOutAt is null or visit.checkOutAt > :rangeStart)")
    Page<AttendanceVisit> findAllOverlappingVisits(@Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd, Pageable pageable);

    @Query("select visit from AttendanceVisit visit where visit.checkInAt < :rangeEnd "
            + "and (visit.checkOutAt is null or visit.checkOutAt > :rangeStart)")
    List<AttendanceVisit> findAllOverlappingVisitsForCalendar(@Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd);
}
