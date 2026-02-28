package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Track-level corner map (telemetry.track_corner_maps).
 * Plan: 13-session-summary-speed-corner-graph.md Phase 3.
 */
@Entity
@Table(name = "track_corner_maps", schema = "telemetry")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackCornerMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "track_id", nullable = false)
    private Short trackId;

    @Column(name = "track_length_m", nullable = false)
    private Integer trackLengthM;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "algorithm_params", columnDefinition = "jsonb")
    private Map<String, Object> algorithmParams;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
