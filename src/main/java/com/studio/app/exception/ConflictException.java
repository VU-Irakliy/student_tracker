package com.studio.app.exception;

/** Thrown when a request violates a uniqueness or state constraint. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
