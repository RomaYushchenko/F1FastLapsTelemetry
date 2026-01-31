package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Session summary entity (telemetry.session_summary table).
 * See: implementation_steps_plan.md § Етап 7.5-7.6.
 */
@Entity
@Table(name = "session_summary", schema = "telemetry")
@IdClass(SessionSummaryId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionSummary {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Column(name = "total_laps")
    private Short totalLaps;

    @Column(name = "best_lap_time_ms")
    private Integer bestLapTimeMs;

    @Column(name = "best_lap_number")
    private Short bestLapNumber;

    @Column(name = "best_sector1_ms")
    private Integer bestSector1Ms;

    @Column(name = "best_sector2_ms")
    private Integer bestSector2Ms;

    @Column(name = "best_sector3_ms")
    private Integer bestSector3Ms;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = Instant.now();
    }
}
