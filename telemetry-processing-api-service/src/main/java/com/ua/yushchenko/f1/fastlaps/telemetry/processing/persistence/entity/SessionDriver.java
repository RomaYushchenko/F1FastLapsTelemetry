package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Driver (participant) display label per session and car index.
 * Used for leaderboard and events. Null driverLabel = fallback "Car N" in service.
 * Block E — Live leaderboard.
 */
@Entity
@Table(name = "session_drivers", schema = "telemetry")
@IdClass(SessionDriverId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDriver {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Column(name = "driver_label", length = 16)
    private String driverLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
