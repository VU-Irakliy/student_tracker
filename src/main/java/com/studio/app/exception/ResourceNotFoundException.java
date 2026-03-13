package com.studio.app.exception;

/** Thrown when a requested resource does not exist or has been soft-deleted. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
