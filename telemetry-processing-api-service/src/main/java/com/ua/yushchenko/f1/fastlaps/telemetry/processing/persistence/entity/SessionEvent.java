package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Persisted session event (F1 Packet Event 3): FTLP, PENA, SCAR, etc.
 * Block E — Session events.
 */
@Entity
@Table(name = "session_events", schema = "telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Column(name = "frame_id", nullable = false)
    private Integer frameId;

    @Column(name = "lap")
    private Short lap;

    @Column(name = "event_code", nullable = false, length = 8)
    private String eventCode;

    @Column(name = "car_index")
    private Short carIndex;

    @Column(name = "detail", columnDefinition = "jsonb")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
