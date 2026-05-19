package com.exercise.delivery.service;

import com.exercise.delivery.model.entities.OutboxEvent;
import com.exercise.delivery.model.enums.OutboxAttachmentStatus;
import com.exercise.delivery.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void saveOutboxEvent(OutboxEvent outboxEvent) {
        outboxEventRepository.save(outboxEvent);
    }

    @Transactional(readOnly = true)
    public List<OutboxEvent> getEvents (OutboxAttachmentStatus status, Pageable pageable) {
        return outboxEventRepository.findByStatusOrderByEventIdAsc(status, pageable);
    }

    @Transactional
    public void updateOutboxStatus(Long eventId, OutboxAttachmentStatus status) {
        outboxEventRepository.updateStatusByEventId(eventId, status);
    }

    @Transactional
    public int deleteEvents(OutboxAttachmentStatus status, LocalDateTime earlierThan) {
        return outboxEventRepository.deleteOldSent(status, earlierThan);
    }
}
