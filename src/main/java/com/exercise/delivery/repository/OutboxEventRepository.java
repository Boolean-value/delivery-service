package com.exercise.delivery.repository;

import com.exercise.delivery.model.entities.OutboxEvent;
import com.exercise.delivery.model.enums.OutboxAttachmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatusOrderByEventIdAsc(OutboxAttachmentStatus status, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE OutboxEvent o
        SET o.status = :status
        WHERE o.eventId = :eventId
    """)
    void updateStatusByEventId(
            @Param("eventId") Long eventId,
            @Param("status") OutboxAttachmentStatus status);

    @Modifying
    @Query("""
        DELETE FROM OutboxEvent o
        WHERE o.status = :status
          AND o.createdAt < :olderThan
    """)
    int deleteOldSent(@Param("status") OutboxAttachmentStatus status,
                      @Param("olderThan") LocalDateTime olderThan);
}
