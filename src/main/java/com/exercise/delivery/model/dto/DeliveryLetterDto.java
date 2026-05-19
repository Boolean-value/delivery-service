package com.exercise.delivery.model.dto;

import com.exercise.delivery.model.enums.LetterType;
import com.exercise.delivery.model.enums.UserType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record DeliveryLetterDto(
        UUID letterId,
        LetterType letterType,
        UserType senderType,
        UUID getterId,
        UserType getterType
) {
}
