package com.exercise.delivery.model.exception;

public class EntityNotFoundRetryableException extends RuntimeException{

    public EntityNotFoundRetryableException(String message) {
        super(message);
    }
}
