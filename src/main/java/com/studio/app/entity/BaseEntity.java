package com.studio.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Base class providing audit timestamps and soft-delete support
 * for all domain entities.
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    /** Timestamp when the record was first persisted. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Timestamp when the record was last updated. */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Soft-delete flag — deleted entities are excluded from all queries by default. */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    /** Initializes audit timestamps before the first insert. */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /** Refreshes update timestamp before each update. */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
