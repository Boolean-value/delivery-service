package com.exercise.delivery.service;

import com.exercise.delivery.model.dto.ReadyLetterDto;
import com.exercise.delivery.model.entities.OutboxEvent;
import com.exercise.delivery.model.enums.OutboxAttachmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxScheduler {

    private final DeliveryLetterService deliveryLetterService;
    private final LetterAndStatusService letterAndStatusService;
    private final OutboxEventService outboxEventService;

    private final KafkaTemplate<String, ReadyLetterDto> getLetterKafkaTemplate;

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.prepare}")
    public void readyLettersPreparing() {
        Pageable pageable = PageRequest.of(
                0, 200, Sort.by("id").ascending());

        List<UUID> letterIds = letterAndStatusService.getAllLetterIdsByNullStatus(pageable);

        if (letterIds.isEmpty()){
            log.debug("[readyLettersPreparing] no letters to prepare");
            return;
        }

        for (UUID letterId : letterIds) {
            deliveryLetterService.prepareForSending(letterId);
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.ready}")
    public void outboxEventProcessing() {
        List<OutboxEvent> events = outboxEventService
                .getEvents(OutboxAttachmentStatus.PENDING, Pageable.ofSize(600));

        if (events.isEmpty()) {
            log.debug("[outboxEventProcessing] no letters to send");
            return;
        }

        for (OutboxEvent event : events) {
            try {
                processSingleEvent(event);
            } catch (InterruptedException e) {
                log.error("[outboxEventProcessing] abort cycle, process is shutdown...");
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.outbox.scheduler.expired}")
    public void deleteOldOutboxEvents() {
        log.debug("[deleteOldEventsOutbox] cleanup started");

        int deleted = outboxEventService.deleteEvents(
                OutboxAttachmentStatus.SENT,
                LocalDateTime.now().minusHours(24));

        log.info("[deleteOldEventsOutbox] deleted event records: {}", deleted);
    }

    private void processSingleEvent(OutboxEvent event) throws InterruptedException {
        log.debug("[outboxEventProcessing] processing event for letterId: {}", event.getLetterId());

        ReadyLetterDto readyLetter = ReadyLetterDto.builder()
                .letterId(event.getLetterId())
                .getterId(event.getGetterId())
                .getterType(event.getGetterType())
                .build();

        ProducerRecord<String, ReadyLetterDto> record = new ProducerRecord<>(
                event.getTopic(),
                event.getGetterType().getPartition(),
                event.getGetterType().name(),
                readyLetter
        );

        try {
            getLetterKafkaTemplate.send(record).get();
            event.setStatus(OutboxAttachmentStatus.SENT);
            outboxEventService.saveOutboxEvent(event);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RetriableException) {
                log.warn("[outboxEventProcessing] temporary Kafka error for event {}: {}",
                        event.getEventId(), cause.getMessage());
            } else {
                log.error("[outboxEventProcessing] fatal error sending event {}. Marking as FAILED",
                        event.getEventId(), cause);
                outboxEventService.updateOutboxStatus(
                        event.getEventId(),
                        OutboxAttachmentStatus.FAILED);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (Exception e) {
            log.error("[outboxEventProcessing] unknown error", e);
        }
    }
}
