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
 * @param <T> payload (DTO) type.
 * @deprecated Use event entities ({@link LapDataEvent}, {@link CarTelemetryEvent}, etc.) that include the DTO instead.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Deprecated
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
