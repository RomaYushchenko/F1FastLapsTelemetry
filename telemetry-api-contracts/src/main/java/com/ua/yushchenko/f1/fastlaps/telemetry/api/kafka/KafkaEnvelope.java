package com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Envelope for Kafka messages (Kafka contract).
 * See: kafka_contracts_f_1_telemetry.md § 4.
 *
 * @param <T> payload type (SessionEventDto, LapDto, CarTelemetryDto, CarStatusDto)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaEnvelope<T> {

    private int schemaVersion;
    private PacketId packetId;
    private long sessionUID;
    private int frameIdentifier;
    private float sessionTime;
    private int carIndex;
    private Instant producedAt;
    private T payload;
}
