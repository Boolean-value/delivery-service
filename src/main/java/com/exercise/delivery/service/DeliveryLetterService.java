package com.exercise.delivery.service;

import com.exercise.delivery.model.dto.DeliveryLetterDto;
import com.exercise.delivery.model.entities.DeliveryLetter;
import com.exercise.delivery.model.entities.DeliveryLetterStatus;
import com.exercise.delivery.model.entities.OutboxEvent;
import com.exercise.delivery.model.enums.DeliveryStatus;
import com.exercise.delivery.model.enums.OutboxAttachmentStatus;
import com.exercise.delivery.model.exception.EntityNotFoundRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeliveryLetterService {

    @Value("${app.kafka.topics.get-topic}")
    private String getLetterTopic;
    private final LetterAndStatusService letterAndStatusService;
    private final OutboxEventService outboxEventService;

    @Transactional
    public void createNewDeliveryLetter(DeliveryLetterDto letterDto) {
        DeliveryLetter deliveryLetter = DeliveryLetter.builder()
                .letterId(letterDto.letterId())
                .getterId(letterDto.getterId())
                .getterType(letterDto.getterType())
                .statuses(new HashSet<>())
                .build();
        letterAndStatusService.saveDeliveryLetter(deliveryLetter);

        DeliveryLetterStatus deliveryStatus = DeliveryLetterStatus.builder()
                .createdTime(LocalDateTime.now())
                .status(DeliveryStatus.NEW)
                .deliveryLetter(deliveryLetter)
                .build();
        letterAndStatusService.saveDeliveryLetterStatus(deliveryStatus);

        deliveryLetter.getStatuses().add(deliveryStatus);
    }

    @Transactional
    public void updateStatusDeliveryLetter(DeliveryLetterDto letterDto, DeliveryStatus status) {
        DeliveryLetter deliveryLetter = letterAndStatusService.getByLetterId(letterDto.letterId());
        if (deliveryLetter == null) {
            log.warn("[updateStatusDeliveryLetter] letter {} with status {} not found",
                    letterDto.letterId(), status);
            throw new EntityNotFoundRetryableException(
                    String.format("Letter %s not found, will retry...", letterDto.letterId()));
        }

        DeliveryLetterStatus deliveryStatus = DeliveryLetterStatus.builder()
                .createdTime(LocalDateTime.now())
                .status(status)
                .deliveryLetter(deliveryLetter)
                .build();
        letterAndStatusService.saveDeliveryLetterStatus(deliveryStatus);

        Set<DeliveryLetterStatus> statuses = deliveryLetter.getStatuses();
        statuses.add(deliveryStatus);
        letterAndStatusService.saveDeliveryLetter(deliveryLetter);
    }

    @Transactional
    public void prepareForSending(UUID letterId) {
        DeliveryLetter letter = letterAndStatusService.getByLetterId(letterId);
        Set<DeliveryLetterStatus> letterStatuses = letter.getStatuses();

        Set<DeliveryStatus> requiredStatuses = Set.of(
                DeliveryStatus.PAYED,
                DeliveryStatus.VALID,
                DeliveryStatus.NEW
        );
        Set<DeliveryStatus> currentStatuses = letterStatuses.stream()
                .map(DeliveryLetterStatus::getStatus)
                .collect(Collectors.toSet());
        if (!currentStatuses.containsAll(requiredStatuses)) {
            return;
        }

        DeliveryLetterStatus readyStatus = DeliveryLetterStatus.builder()
                .createdTime(LocalDateTime.now())
                .status(DeliveryStatus.READY)
                .deliveryLetter(letter)
                .build();
        letterStatuses.add(readyStatus);

        letter.setStatus(DeliveryStatus.READY);

        letterAndStatusService.saveDeliveryLetter(letter);

        OutboxEvent outboxEvent = OutboxEvent.builder()
                .topic(getLetterTopic)
                .letterId(letter.getLetterId())
                .getterId(letter.getGetterId())
                .getterType(letter.getGetterType())
                .createdAt(LocalDateTime.now())
                .status(OutboxAttachmentStatus.PENDING)
                .build();

        outboxEventService.saveOutboxEvent(outboxEvent);
    }
}
