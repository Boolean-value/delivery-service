package com.exercise.delivery.model.dto;

import com.exercise.delivery.model.enums.UserType;
import lombok.Builder;

import java.util.UUID;

@Builder
public record ReadyLetterDto(
        UUID letterId,
        UUID getterId,
        UserType getterType
) {
}
