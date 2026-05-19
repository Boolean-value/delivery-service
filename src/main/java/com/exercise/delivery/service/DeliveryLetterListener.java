package com.exercise.delivery.service;

import com.exercise.delivery.model.dto.DeliveryLetterDto;
import com.exercise.delivery.model.enums.DeliveryStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class DeliveryLetterListener {

    private final DeliveryLetterService deliveryLetterService;

    @KafkaListener(
            topics = "${app.kafka.topics.delivery-topic}",
            containerFactory = "deliveryKafkaListenerFactory")
    public void deliveryLetter(@Payload DeliveryLetterDto payload,
                               @Header(required = false, name = "TYPE") String headerType) {

        switch(headerType.toUpperCase()) {
            case "CREATE" -> {
                deliveryLetterService.createNewDeliveryLetter(payload);
            }
            case "PAY" -> {
                deliveryLetterService.updateStatusDeliveryLetter(payload, DeliveryStatus.PAYED);
            }
            case "VALID" -> {
                deliveryLetterService.updateStatusDeliveryLetter(payload, DeliveryStatus.VALID);
            }
            default -> {
                log.warn("[deliveryLetter] unknown header type: {}", headerType);
            }
        };
    }
}
