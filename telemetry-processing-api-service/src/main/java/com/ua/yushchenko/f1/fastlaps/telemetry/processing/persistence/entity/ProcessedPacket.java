package com.ua.yushchenko.f1.fastlaps.telemetry.processing.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Idempotency tracking: records processed packets to prevent duplicate processing.
 * See: implementation_steps_plan.md § Етап 5.2.
 */
@Entity
@Table(name = "processed_packets", schema = "telemetry")
@IdClass(ProcessedPacketId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedPacket {

    @Id
    @Column(name = "session_uid", nullable = false)
    private Long sessionUid;

    @Id
    @Column(name = "frame_identifier", nullable = false)
    private Integer frameIdentifier;

    @Id
    @Column(name = "packet_id", nullable = false)
    private Short packetId;

    @Id
    @Column(name = "car_index", nullable = false)
    private Short carIndex;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @PrePersist
    protected void onCreate() {
        processedAt = Instant.now();
    }
}
