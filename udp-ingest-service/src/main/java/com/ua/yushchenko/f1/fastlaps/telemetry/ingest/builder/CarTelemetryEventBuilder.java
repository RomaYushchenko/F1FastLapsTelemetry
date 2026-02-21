package com.ua.yushchenko.f1.fastlaps.telemetry.ingest.builder;

import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryDto;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.CarTelemetryEvent;
import com.ua.yushchenko.f1.fastlaps.telemetry.api.kafka.PacketId;
import com.ua.yushchenko.f1.fastlaps.telemetry.udp.core.packet.PacketHeader;

import java.time.Instant;

/**
 * Builds {@link CarTelemetryEvent} from packet header and telemetry payload.
 */
public final class CarTelemetryEventBuilder {

    private CarTelemetryEventBuilder() {
    }

    /**
     * Build car telemetry event with schema version 1 and producedAt set to now.
     */
    public static CarTelemetryEvent build(PacketHeader header, CarTelemetryDto payload) {
        return CarTelemetryEvent.builder()
                .schemaVersion(1)
                .packetId(PacketId.CAR_TELEMETRY)
                .sessionUID(header.getSessionUID())
                .frameIdentifier((int) header.getFrameIdentifier())
                .sessionTime(header.getSessionTime())
                .carIndex(header.getPlayerCarIndex())
                .producedAt(Instant.now())
                .payload(payload)
                .build();
    }
}
