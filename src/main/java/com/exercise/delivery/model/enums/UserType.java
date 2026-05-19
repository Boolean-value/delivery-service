package com.exercise.delivery.model.enums;

import lombok.Getter;

@Getter
public enum UserType {
    A(1), B(2), C(3), D(4);

    private final int partition;

    UserType(int i) {
        this.partition = i;
    }
}
