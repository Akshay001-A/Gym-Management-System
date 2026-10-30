
package com.gym.payment.repository;

import com.gym.payment.entity.WebhookEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WebhookEventRepository
        extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    @Modifying
    @Query(value = """
            INSERT INTO razorpay_webhook_events
                (event_id, event_type, received_at,
                 processing_status, attempt_count)
            VALUES
                (:eventId, :eventType, CURRENT_TIMESTAMP,
                 'RECEIVED', 0)
            ON CONFLICT (event_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM WebhookEvent e
            WHERE e.eventId = :eventId
            """)
    Optional<WebhookEvent> findByEventIdForUpdate(
            @Param("eventId") String eventId
    );
}
