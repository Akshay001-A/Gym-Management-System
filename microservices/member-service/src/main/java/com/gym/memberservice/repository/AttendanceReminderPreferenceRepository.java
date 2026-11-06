package com.gym.memberservice.repository;

import com.gym.memberservice.entity.AttendanceReminderPreference;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceReminderPreferenceRepository extends JpaRepository<AttendanceReminderPreference, Long> {
    List<AttendanceReminderPreference> findAllByEnabledTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select preference from AttendanceReminderPreference preference where preference.memberId = :memberId")
    Optional<AttendanceReminderPreference> findByMemberIdForUpdate(@Param("memberId") Long memberId);
}
