package com.studio.app.exception;

/** Thrown when business logic rules are violated in a request. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
